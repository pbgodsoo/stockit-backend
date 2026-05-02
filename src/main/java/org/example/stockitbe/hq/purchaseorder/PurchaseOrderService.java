package org.example.stockitbe.hq.purchaseorder;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.infrastructure.InfrastructureRepository;
import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private static final String HQ_MANAGER_ACTOR = "본사 관리자";
    private static final String WAREHOUSE_MANAGER_ACTOR = "창고 관리자";
    private static final DateTimeFormatter CODE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository itemRepository;
    private final PurchaseOrderStatusHistoryRepository historyRepository;
    private final VendorRepository vendorRepository;
    private final VendorProductRepository vendorProductRepository;
    private final InfrastructureRepository infrastructureRepository;
    private final ProductSkuRepository productSkuRepository;

    @Transactional(readOnly = true)
    public List<PurchaseOrderDto.ListRes> findAll(String vendorCode, PurchaseOrderStatus status,
                                                    LocalDate from, LocalDate to) {
        Long vendorIdFilter = null;
        if (vendorCode != null && !vendorCode.isBlank()) {
            Vendor vendor = lookupVendor(vendorCode);
            vendorIdFilter = vendor.getId();
        }

        Specification<PurchaseOrder> spec = buildSpec(vendorIdFilter, status, from, to);
        List<PurchaseOrder> orders = purchaseOrderRepository.findAll(spec);
        if (orders.isEmpty()) {
            return List.of();
        }

        // vendor / warehouse / itemCount 매핑
        Set<Long> vendorIds = orders.stream().map(PurchaseOrder::getVendorId).collect(Collectors.toSet());
        Map<Long, Vendor> vendorMap = vendorRepository.findAllById(vendorIds).stream()
                .collect(Collectors.toMap(Vendor::getId, v -> v));

        Set<Long> warehouseIds = orders.stream().map(PurchaseOrder::getWarehouseId).collect(Collectors.toSet());
        Map<Long, String> warehouseCodeById = infrastructureRepository.findAllById(warehouseIds).stream()
                .filter(infra -> infra.getLocationType() == LocationType.WAREHOUSE)
                .collect(Collectors.toMap(Infrastructure::getId, Infrastructure::getCode));

        Set<Long> orderIds = orders.stream().map(PurchaseOrder::getId).collect(Collectors.toSet());
        // batch 1회 조회 결과를 itemCountMap + productNamesMap 두 가지로 활용 (쿼리 0추가)
        List<PurchaseOrderItem> allItems = itemRepository.findAllByPurchaseOrderIdIn(orderIds);
        Map<Long, Long> itemCountMap = allItems.stream()
                .collect(Collectors.groupingBy(PurchaseOrderItem::getPurchaseOrderId, Collectors.counting()));
        Map<Long, List<String>> productNamesMap = allItems.stream()
                .collect(Collectors.groupingBy(
                        PurchaseOrderItem::getPurchaseOrderId,
                        Collectors.mapping(PurchaseOrderItem::getProductName, Collectors.toList())));

        return orders.stream()
                .map(po -> {
                    Vendor vendor = vendorMap.get(po.getVendorId());
                    if (vendor == null) {
                        throw BaseException.from(BaseResponseStatus.VENDOR_NOT_FOUND);
                    }
                    int count = itemCountMap.getOrDefault(po.getId(), 0L).intValue();
                    List<String> names = productNamesMap.getOrDefault(po.getId(), List.of());
                    String warehouseCode = warehouseCodeById.getOrDefault(po.getWarehouseId(), "");
                    return PurchaseOrderDto.ListRes.from(po, vendor, warehouseCode, count, names);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PurchaseOrderDto.DetailRes findByCode(String code) {
        PurchaseOrder po = lookupPurchaseOrder(code);
        return buildDetailRes(po);
    }

    @Transactional
    public PurchaseOrderDto.DetailRes create(PurchaseOrderDto.CreateReq req) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_EMPTY_ITEMS);
        }

        Vendor vendor = lookupVendor(req.getVendorCode());
        Infrastructure warehouse = lookupWarehouse(req.getWarehouseCode());

        // items 의 vendorProduct 모두 조회 + vendor 일치 검증
        List<VendorProduct> vendorProducts = req.getItems().stream()
                .map(itemReq -> {
                    VendorProduct vp = lookupVendorProduct(itemReq.getVendorProductCode());
                    if (!vp.getVendorId().equals(vendor.getId())) {
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

        long totalAmount = 0L;
        for (int i = 0; i < req.getItems().size(); i++) {
            totalAmount += skus.get(i).getUnitPrice() * req.getItems().get(i).getQuantity();
        }

        String code = generateCode();
        PurchaseOrder entity = req.toEntity(vendor, warehouse, code, totalAmount);
        PurchaseOrder saved = purchaseOrderRepository.save(entity);

        // items 저장 (purchaseOrderId 채움)
        List<PurchaseOrderItem> items = new ArrayList<>();
        for (int i = 0; i < req.getItems().size(); i++) {
            PurchaseOrderItem item = req.getItems().get(i).toEntity(saved.getId(), vendorProducts.get(i), skus.get(i));
            items.add(item);
        }
        itemRepository.saveAll(items);

        appendHistory(saved, null);

        return buildDetailRes(saved);
    }

    @Transactional
    public PurchaseOrderDto.DetailRes update(String code, PurchaseOrderDto.UpdateReq req) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_EMPTY_ITEMS);
        }

        PurchaseOrder po = lookupPurchaseOrder(code);
        if (po.getStatus() != PurchaseOrderStatus.PENDING) {
            throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_INVALID_STATUS_TRANSITION);
        }

        Infrastructure warehouse = lookupWarehouse(req.getWarehouseCode());

        // items 의 vendorProduct 검증 (vendor 일치)
        List<VendorProduct> vendorProducts = req.getItems().stream()
                .map(itemReq -> {
                    VendorProduct vp = lookupVendorProduct(itemReq.getVendorProductCode());
                    if (!vp.getVendorId().equals(po.getVendorId())) {
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

        // 기존 items 삭제
        itemRepository.deleteAllByPurchaseOrderId(po.getId());
        itemRepository.flush();

        // 신규 items 빌드/save
        List<PurchaseOrderItem> newItems = new ArrayList<>();
        for (int i = 0; i < req.getItems().size(); i++) {
            newItems.add(req.getItems().get(i).toEntity(po.getId(), vendorProducts.get(i), skus.get(i)));
        }
        itemRepository.saveAll(newItems);

        // 창고 스냅샷 갱신 (서버 lookup 결과) + totalAmount 재계산
        po.updateLogistics(warehouse.getId(), warehouse.getName());
        po.recalculateTotalAmount(newItems);

        return buildDetailRes(po);
    }

    @Transactional
    public PurchaseOrderDto.DetailRes approve(String code) {
        PurchaseOrder po = lookupPurchaseOrder(code);
        po.markApproved();
        appendHistory(po, null);
        return buildDetailRes(po);
    }

    @Transactional
    public PurchaseOrderDto.DetailRes startShipping(String code) {
        PurchaseOrder po = lookupPurchaseOrder(code);
        po.markShipping();
        appendHistory(po, null);
        return buildDetailRes(po);
    }

    @Transactional
    public PurchaseOrderDto.DetailRes deliver(String code) {
        PurchaseOrder po = lookupPurchaseOrder(code);
        po.markDelivered();
        appendHistory(po, null);
        return buildDetailRes(po);
    }

    @Transactional
    public PurchaseOrderDto.DetailRes complete(String code) {
        PurchaseOrder po = lookupPurchaseOrder(code);
        po.markCompleted();
        appendHistory(po, null);
        return buildDetailRes(po);
    }

    @Transactional
    public PurchaseOrderDto.DetailRes cancel(String code, PurchaseOrderDto.CancelReq req) {
        if (req.getCancelReason() == null || req.getCancelReason().isBlank()) {
            throw BaseException.from(BaseResponseStatus.PURCHASE_ORDER_CANCEL_REASON_REQUIRED);
        }
        PurchaseOrder po = lookupPurchaseOrder(code);
        po.markRejected(req.getCancelReason());
        appendHistory(po, req.getCancelReason());
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
     * 코드 자동 생성 — PO-{YYYYMMDD}-{NNN}.
     * NNN 은 같은 날 prefix count + 1 (3자리 zero-pad).
     */
    private String generateCode() {
        String today = LocalDate.now().format(CODE_DATE_FORMAT);
        String prefix = "PO-" + today + "-";
        long seq = purchaseOrderRepository.countByCodeStartingWith(prefix) + 1;
        return String.format("%s%03d", prefix, seq);
    }

    /**
     * 진행 이력 한 행 추가. changedByName 은 도메인 책임자 기준 분기:
     *   - APPROVED / SHIPPING / DELIVERED : "담당자명 (회사명)" 형식 — 발주 시점 거래처 스냅샷
     *     (실제 트리거는 SYS-001 배치지만 자동화는 구현 디테일이라 도메인 이력에 노출하지 않음, ADR-013/019)
     *     실무 ERP 표준 — 법적 주체(회사) + 실무 처리자(담당자) 둘 다 노출.
     *   - COMPLETED            : 입고 확정은 창고 관리자 책임
     *   - PENDING(생성) / REJECTED(취소) : 본사 관리자
     * 인증 도입(ADR-011) 후 본사·창고 관리자명은 실제 사용자명으로 교체.
     */
    private void appendHistory(PurchaseOrder po, String note) {
        String changedByName = switch (po.getStatus()) {
            case APPROVED, SHIPPING, DELIVERED -> po.getVendorContactName() + " (" + po.getVendorName() + ")";
            case COMPLETED -> WAREHOUSE_MANAGER_ACTOR;
            default -> HQ_MANAGER_ACTOR;
        };
        PurchaseOrderStatusHistory entry = PurchaseOrderStatusHistory.builder()
                .purchaseOrderId(po.getId())
                .status(po.getStatus())
                .changedByName(changedByName)
                .note(note)
                .build();
        historyRepository.save(entry);
    }

    private PurchaseOrderDto.DetailRes buildDetailRes(PurchaseOrder po) {
        Vendor vendor = vendorRepository.findById(po.getVendorId())
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.VENDOR_NOT_FOUND));

        // warehouse code lookup (응답용 — id → code 변환)
        String warehouseCode = infrastructureRepository.findById(po.getWarehouseId())
                .filter(infra -> infra.getLocationType() == LocationType.WAREHOUSE)
                .map(Infrastructure::getCode)
                .orElse("");

        List<PurchaseOrderItem> items = itemRepository.findAllByPurchaseOrderId(po.getId());
        List<PurchaseOrderStatusHistory> history = historyRepository.findAllByPurchaseOrderIdOrderByChangedAtAsc(po.getId());

        // vendorProductId → vendorProductCode 맵 (FE 친화 응답용)
        Set<Long> vendorProductIds = items.stream().map(PurchaseOrderItem::getVendorProductId).collect(Collectors.toSet());
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
                predicates.add(cb.equal(root.get("vendorId"), vendorId));
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
