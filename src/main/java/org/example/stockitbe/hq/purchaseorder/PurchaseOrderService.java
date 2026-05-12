package org.example.stockitbe.hq.purchaseorder;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.infrastructure.InfrastructureRepository;
import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
import org.example.stockitbe.hq.inventory.InventoryService;
import org.example.stockitbe.hq.product.ProductSkuRepository;
import org.example.stockitbe.hq.product.model.ProductSku;
import org.example.stockitbe.hq.product.model.ProductStatus;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrder;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderDto;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderItem;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderStatus;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderStatusHistory;
import org.example.stockitbe.hq.vendor.VendorProductRepository;
import org.example.stockitbe.hq.vendor.VendorRepository;
import org.example.stockitbe.hq.vendor.model.Vendor;
import org.example.stockitbe.hq.vendor.model.VendorProduct;
import org.example.stockitbe.user.model.AuthUserDetails;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private static final DateTimeFormatter CODE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository itemRepository;
    private final PurchaseOrderStatusHistoryRepository historyRepository;
    private final VendorRepository vendorRepository;
    private final VendorProductRepository vendorProductRepository;
    private final InfrastructureRepository infrastructureRepository;
    private final ProductSkuRepository productSkuRepository;
    private final InventoryService inventoryService;

    @Transactional(readOnly = true)
    public List<PurchaseOrderDto.ListRes> findAll(String vendorCode, PurchaseOrderStatus status,
                                                    LocalDate from, LocalDate to) {
        Long vendorIdFilter = null;
        if (vendorCode != null && !vendorCode.isBlank()) {
            Vendor vendor = lookupVendor(vendorCode);
            vendorIdFilter = vendor.getId();
        }

        Specification<PurchaseOrder> spec = buildSpec(vendorIdFilter, status, from, to);
        // @EntityGraph(vendor, warehouse) — 단일 LEFT JOIN 으로 fetch.
        List<PurchaseOrder> orders = purchaseOrderRepository.findAll(spec);
        if (orders.isEmpty()) {
            return List.of();
        }

        Set<Long> orderIds = orders.stream().map(PurchaseOrder::getId).collect(Collectors.toSet());
        // batch 1회 조회 결과를 itemCountMap + productNamesMap 두 가지로 활용 (쿼리 0추가)
        List<PurchaseOrderItem> allItems = itemRepository.findAllByPurchaseOrderIdIn(orderIds);
        Map<Long, Long> itemCountMap = allItems.stream()
                .collect(Collectors.groupingBy(it -> it.getPurchaseOrder().getId(), Collectors.counting()));
        Map<Long, List<String>> productNamesMap = allItems.stream()
                .collect(Collectors.groupingBy(
                        it -> it.getPurchaseOrder().getId(),
                        Collectors.mapping(PurchaseOrderItem::getProductName, Collectors.toList())));

        return orders.stream()
                .map(po -> {
                    int count = itemCountMap.getOrDefault(po.getId(), 0L).intValue();
                    List<String> names = productNamesMap.getOrDefault(po.getId(), List.of());
                    return PurchaseOrderDto.ListRes.from(po, po.getVendor(), po.getWarehouse().getCode(), count, names);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PurchaseOrderDto.DetailRes findByCode(String code) {
        PurchaseOrder po = lookupPurchaseOrder(code);
        return buildDetailRes(po);
    }

    @Transactional
    public PurchaseOrderDto.DetailRes create(PurchaseOrderDto.CreateReq req, AuthUserDetails me) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_EMPTY_ITEMS);
        }

        Vendor vendor = lookupVendor(req.getVendorCode());
        Infrastructure warehouse = lookupWarehouse(req.getWarehouseCode());

        // items 의 vendorProduct 모두 조회 + vendor 일치 검증 (한 발주 = 한 vendor 정책)
        List<VendorProduct> vendorProducts = req.getItems().stream()
                .map(itemReq -> {
                    VendorProduct vp = lookupVendorProduct(itemReq.getVendorProductCode());
                    if (!vp.getVendor().getId().equals(vendor.getId())) {
                        throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_VENDOR_PRODUCT_MISMATCH);
                    }
                    return vp;
                })
                .toList();

        // items 의 SKU 모두 조회 + vp.productCode 일치 검증
        List<ProductSku> skus = req.getItems().stream()
                .map(itemReq -> lookupSku(itemReq.getSkuCode()))
                .toList();
        for (int i = 0; i < req.getItems().size(); i++) {
            if (!skus.get(i).getProductCode().equals(vendorProducts.get(i).getProductCode())) {
                throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_SKU_PRODUCT_MISMATCH);
            }
        }

        String code = generateCode();
        PurchaseOrder saved = buildAndSaveOrder(
                vendor, warehouse, req.getItems(), vendorProducts, skus,
                code, req.getMemberId(), req.getMemberName(), me);

        return buildDetailRes(saved);
    }

    /**
     * 멀티 공급처 장바구니 → 공급처별 자동 분할 발주 N건 생성.
     *
     * vendorProductCode → Vendor 매핑으로 자동 그룹핑. 1건이라도 실패하면 단일 @Transactional 롤백으로
     * N건 모두 미생성. FE 는 단일 items 배열만 보낸다 (vendorCode 없음).
     *
     * 응답 orders 는 vendorName ASC 정렬 (FE 표시 안정성).
     */
    @Transactional
    public PurchaseOrderDto.BatchCreateRes createBatch(PurchaseOrderDto.BatchCreateReq req, AuthUserDetails me) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_BATCH_EMPTY);
        }

        Infrastructure warehouse = lookupWarehouse(req.getWarehouseCode());

        int n = req.getItems().size();
        List<VendorProduct> vendorProducts = new ArrayList<>(n);
        List<ProductSku> skus = new ArrayList<>(n);
        for (PurchaseOrderDto.ItemReq itemReq : req.getItems()) {
            VendorProduct vp = lookupVendorProduct(itemReq.getVendorProductCode());
            ProductSku sku = lookupSku(itemReq.getSkuCode());
            if (!sku.getProductCode().equals(vp.getProductCode())) {
                throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_SKU_PRODUCT_MISMATCH);
            }
            vendorProducts.add(vp);
            skus.add(sku);
        }

        // vendor.id 로 그룹핑 — LinkedHashMap 으로 첫 등장 순서 보존 (디버깅 친화)
        Map<Long, List<Integer>> groupedIndices = new LinkedHashMap<>();
        Map<Long, Vendor> vendorById = new HashMap<>();
        for (int i = 0; i < n; i++) {
            Vendor vendor = vendorProducts.get(i).getVendor();
            Long vid = vendor.getId();
            vendorById.putIfAbsent(vid, vendor);
            groupedIndices.computeIfAbsent(vid, k -> new ArrayList<>()).add(i);
        }

        // 코드 시퀀스 base 1회 계산 — 같은 트랜잭션 내 미커밋 INSERT 가 countByCodeStartingWith 에 잡히지 않아
        // generateCode() N번 호출 시 모두 같은 NNNNN 받아 UNIQUE 제약 위반. base + i 인라인으로 회피.
        String today = LocalDate.now().format(CODE_DATE_FORMAT);
        String prefix = "PO-" + today + "-";
        long baseSeq = purchaseOrderRepository.countByCodeStartingWith(prefix) + 1;

        List<PurchaseOrder> savedOrders = new ArrayList<>();
        int groupIdx = 0;
        for (Map.Entry<Long, List<Integer>> entry : groupedIndices.entrySet()) {
            Vendor vendor = vendorById.get(entry.getKey());
            List<Integer> indices = entry.getValue();
            List<PurchaseOrderDto.ItemReq> groupItems = new ArrayList<>(indices.size());
            List<VendorProduct> groupVps = new ArrayList<>(indices.size());
            List<ProductSku> groupSkus = new ArrayList<>(indices.size());
            for (Integer idx : indices) {
                groupItems.add(req.getItems().get(idx));
                groupVps.add(vendorProducts.get(idx));
                groupSkus.add(skus.get(idx));
            }
            String code = String.format("%s%05d", prefix, baseSeq + groupIdx);
            PurchaseOrder saved = buildAndSaveOrder(
                    vendor, warehouse, groupItems, groupVps, groupSkus,
                    code, req.getMemberId(), req.getMemberName(), me);
            savedOrders.add(saved);
            groupIdx++;
        }

        savedOrders.sort(Comparator.comparing(PurchaseOrder::getVendorName));

        List<PurchaseOrderDto.DetailRes> details = savedOrders.stream()
                .map(this::buildDetailRes)
                .toList();
        long totalAmount = savedOrders.stream().mapToLong(PurchaseOrder::getTotalAmount).sum();
        int itemCount = savedOrders.stream().mapToInt(po -> po.getItems().size()).sum();

        return PurchaseOrderDto.BatchCreateRes.builder()
                .orders(details)
                .vendorCount(savedOrders.size())
                .itemCount(itemCount)
                .totalAmount(totalAmount)
                .build();
    }

    /**
     * 발주 한 건 entity 빌드 + save + REQUESTED 이력 추가.
     * 단일 create / batch createBatch 두 흐름에서 공유 — @Transactional self-invocation 회피 위해
     * private helper 추출 (Spring AOP 가 같은 클래스 self-invocation 에서 트랜잭션 전파 보장하지 않을 수 있음).
     */
    private PurchaseOrder buildAndSaveOrder(
            Vendor vendor,
            Infrastructure warehouse,
            List<PurchaseOrderDto.ItemReq> items,
            List<VendorProduct> vendorProducts,
            List<ProductSku> skus,
            String code,
            String memberId,
            String memberName,
            AuthUserDetails me) {
        PurchaseOrder entity = PurchaseOrder.builder()
                .code(code)
                .vendor(vendor)
                .vendorName(vendor.getName())
                .vendorContactName(vendor.getContactName())
                .warehouse(warehouse)
                .warehouseName(warehouse.getName())
                .memberId(memberId)
                .memberName(memberName)
                .totalAmount(0L)   // replaceItems 가 재계산
                .build();

        List<PurchaseOrderItem> poItems = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            poItems.add(items.get(i).toEntity(vendorProducts.get(i), skus.get(i)));
        }
        entity.replaceItems(poItems);

        PurchaseOrder saved = purchaseOrderRepository.save(entity);
        appendHistory(saved, null, me);
        return saved;
    }

    @Transactional
    public PurchaseOrderDto.DetailRes update(String code, PurchaseOrderDto.UpdateReq req) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_EMPTY_ITEMS);
        }

        PurchaseOrder po = lookupPurchaseOrder(code);
        if (po.getStatus() != PurchaseOrderStatus.REQUESTED) {
            throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_INVALID_STATUS_TRANSITION);
        }

        Infrastructure warehouse = lookupWarehouse(req.getWarehouseCode());

        // items 의 vendorProduct 검증 (vendor 일치)
        List<VendorProduct> vendorProducts = req.getItems().stream()
                .map(itemReq -> {
                    VendorProduct vp = lookupVendorProduct(itemReq.getVendorProductCode());
                    if (!vp.getVendor().getId().equals(po.getVendor().getId())) {
                        throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_VENDOR_PRODUCT_MISMATCH);
                    }
                    return vp;
                })
                .toList();

        // items 의 SKU 검증 (vp.productCode 일치)
        List<ProductSku> skus = req.getItems().stream()
                .map(itemReq -> lookupSku(itemReq.getSkuCode()))
                .toList();
        for (int i = 0; i < req.getItems().size(); i++) {
            if (!skus.get(i).getProductCode().equals(vendorProducts.get(i).getProductCode())) {
                throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_SKU_PRODUCT_MISMATCH);
            }
        }

        // 신규 items 빌드 후 부모 도메인 메소드에 위임 —
        // orphanRemoval=true 가 옛 items DELETE, cascade=ALL 이 신규 items INSERT, totalAmount 재계산도 한꺼번에.
        List<PurchaseOrderItem> newItems = new ArrayList<>();
        for (int i = 0; i < req.getItems().size(); i++) {
            newItems.add(req.getItems().get(i).toEntity(vendorProducts.get(i), skus.get(i)));
        }
        po.replaceItems(newItems);

        // 창고 매핑 + 스냅샷 갱신 (서버 lookup 결과)
        po.updateLogistics(warehouse, warehouse.getName());

        return buildDetailRes(po);
    }

    @Transactional
    public PurchaseOrderDto.DetailRes approve(String code) {
        PurchaseOrder po = lookupPurchaseOrder(code);
        po.markApproved();
        appendHistory(po, null, null);
        return buildDetailRes(po);
    }

    @Transactional
    public PurchaseOrderDto.DetailRes readyToShip(String code) {
        PurchaseOrder po = lookupPurchaseOrder(code);
        po.markReadyToShip();
        appendHistory(po, null, null);
        return buildDetailRes(po);
    }

    @Transactional
    public PurchaseOrderDto.DetailRes startInTransit(String code) {
        PurchaseOrder po = lookupPurchaseOrder(code);
        po.markInTransit();
        appendHistory(po, null, null);
        // 발주 ↔ 인벤토리 연결 — 가용재고 += 발주 수량 (도착 전 예약). PR #173 위치로 복귀
        // (이전 사이클에서 inbound 로 잠시 이동했다가 ERP 표준 정정으로 다시 PO 책임으로).
        itemRepository.findAllByPurchaseOrderId(po.getId())
                .forEach(it -> inventoryService.increaseAvailable(po.getWarehouse().getId(), it.getSkuCode(), it.getQuantity()));
        return buildDetailRes(po);
    }

    @Transactional
    public PurchaseOrderDto.DetailRes arrive(String code) {
        PurchaseOrder po = lookupPurchaseOrder(code);
        po.markArrived();
        appendHistory(po, null, null);
        return buildDetailRes(po);
    }

    @Transactional
    public PurchaseOrderDto.DetailRes complete(String code, AuthUserDetails me) {
        PurchaseOrder po = lookupPurchaseOrder(code);
        po.markCompleted();
        appendHistory(po, null, me);
        // 인벤토리 hook 제거 — WhInboundService.confirmInbound 가 책임 (PR #173 위치 이동).
        // 이 메소드의 외부 호출처는 step 4 에서 WhInboundController 로 갈아끼워질 때 사라짐.
        return buildDetailRes(po);
    }

    /**
     * inbound.confirmInbound 가 호출하는 PO mirror entry point.
     * inbound 가 진실 원천이 되는 입고 확정 시점에 PO 도 COMPLETED 박음 (본사 리스트 "종료" 표시).
     */
    @Transactional
    public void completeFromInbound(String code, AuthUserDetails me) {
        PurchaseOrder po = lookupPurchaseOrder(code);
        po.markCompleted();
        appendHistory(po, null, me);
        // 인벤토리 갱신 X — inbound 가 책임
    }

    @Transactional
    public PurchaseOrderDto.DetailRes cancel(String code, PurchaseOrderDto.CancelReq req, AuthUserDetails me) {
        if (req.getCancelReason() == null || req.getCancelReason().isBlank()) {
            throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_CANCEL_REASON_REQUIRED);
        }
        PurchaseOrder po = lookupPurchaseOrder(code);
        po.markCancelled(req.getCancelReason());
        appendHistory(po, req.getCancelReason(), me);
        return buildDetailRes(po);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Vendor lookupVendor(String vendorCode) {
        return vendorRepository.findByCode(vendorCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.VENDOR_NOT_FOUND));
    }

    private PurchaseOrder lookupPurchaseOrder(String code) {
        return purchaseOrderRepository.findByCode(code)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.PURCHASE_ORDER_NOT_FOUND));
    }

    private VendorProduct lookupVendorProduct(String vendorProductCode) {
        return vendorProductRepository.findByCode(vendorProductCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.VENDOR_PRODUCT_NOT_FOUND));
    }

    private Infrastructure lookupWarehouse(String code) {
        if (code == null || code.isBlank()) {
            throw BaseException.from(BaseResponseStatus.WAREHOUSE_NOT_FOUND);
        }
        Infrastructure warehouse = infrastructureRepository.findByCode(code)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.WAREHOUSE_NOT_FOUND));
        if (warehouse.getLocationType() != LocationType.WAREHOUSE) {
            throw BaseException.from(BaseResponseStatus.WAREHOUSE_NOT_FOUND);
        }
        return warehouse;
    }

    private ProductSku lookupSku(String skuCode) {
        ProductSku sku = productSkuRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.PRODUCT_SKU_NOT_FOUND));
        if (sku.getStatus() != ProductStatus.ACTIVE) {
            throw BaseException.from(BaseResponseStatus.PRODUCT_SKU_NOT_FOUND);
        }
        return sku;
    }

    /**
     * 코드 자동 생성 — PO-{YYYYMMDD}-{NNNNN}.
     * NNNNN 은 같은 날 prefix count + 1 (5자리 zero-pad).
     */
    private String generateCode() {
        String today = LocalDate.now().format(CODE_DATE_FORMAT);
        String prefix = "PO-" + today + "-";
        long seq = purchaseOrderRepository.countByCodeStartingWith(prefix) + 1;
        return String.format("%s%05d", prefix, seq);
    }

    /**
     * 진행 이력 한 행 추가. changedByName 은 도메인 책임자 기준 분기:
     *   - APPROVED / READY_TO_SHIP / IN_TRANSIT / ARRIVED : "담당자명 (회사명)" 형식 — 발주 시점 공급처 스냅샷.
     *     실제 트리거는 SYS-001 배치(거래처 책임 단계 자동화) — 자동화는 구현 디테일이라 도메인 이력에 노출하지 않음 (ADR-013/019).
     *     실무 ERP 표준 — 법적 주체(회사) + 실무 처리자(담당자) 둘 다 노출. 배치 호출이라 인증 사용자 없음 (me=null).
     *   - REQUESTED / COMPLETED / CANCELLED : 인증 사용자(me) 의 실명. me 가 null 이면 NOT_AUTHENTICATED.
     *     본사 작성/창고 입고 확정/본사 취소 — 인증된 담당자의 책임 단계.
     */
    private void appendHistory(PurchaseOrder po, String note, AuthUserDetails me) {
        String changedByName = switch (po.getStatus()) {
            case APPROVED, READY_TO_SHIP, IN_TRANSIT, ARRIVED ->
                    po.getVendorContactName() + " (" + po.getVendorName() + ")";
            case REQUESTED, COMPLETED, CANCELLED -> {
                if (me == null) {
                    throw BaseException.from(BaseResponseStatus.NOT_AUTHENTICATED);
                }
                yield me.getName() + " (" + me.getLocationName() + ")";
            }
        };
        // cascade=PERSIST 가 자동 INSERT — historyRepository.save 직접 호출 폐기.
        po.appendHistory(changedByName, note);
    }

    private PurchaseOrderDto.DetailRes buildDetailRes(PurchaseOrder po) {
        // @EntityGraph(vendor, warehouse) on findByCode — proxy 가 아닌 실 entity. 추가 lookup 불필요.
        Vendor vendor = po.getVendor();
        String warehouseCode = po.getWarehouse().getCode();

        List<PurchaseOrderItem> items = itemRepository.findAllByPurchaseOrderId(po.getId());
        List<PurchaseOrderStatusHistory> history = historyRepository.findAllByPurchaseOrderIdOrderByChangedAtAsc(po.getId());

        // vendorProductId → vendorProductCode 맵 (FE 친화 응답용)
        Set<Long> vendorProductIds = items.stream().map(it -> it.getVendorProduct().getId()).collect(Collectors.toSet());
        Map<Long, String> codeMap = new HashMap<>();
        if (!vendorProductIds.isEmpty()) {
            vendorProductRepository.findAllById(vendorProductIds)
                    .forEach(vp -> codeMap.put(vp.getId(), vp.getCode()));
        }

        return PurchaseOrderDto.DetailRes.from(po, vendor, warehouseCode, items, history, codeMap);
    }

    private Specification<PurchaseOrder> buildSpec(Long vendorId, PurchaseOrderStatus status,
                                                     LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (vendorId != null) {
                // @ManyToOne path traversal — Specification 이 자동으로 vendor_id 컬럼 비교 SQL 로 변환.
                predicates.add(cb.equal(root.get("vendor").get("id"), vendorId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                Date fromDate = Date.from(from.atStartOfDay(ZoneId.systemDefault()).toInstant());
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (to != null) {
                Date toDate = Date.from(to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
                predicates.add(cb.lessThan(root.get("createdAt"), toDate));
            }
            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
