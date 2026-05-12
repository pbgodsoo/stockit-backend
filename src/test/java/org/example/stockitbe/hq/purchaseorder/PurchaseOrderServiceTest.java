package org.example.stockitbe.hq.purchaseorder;

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
import org.example.stockitbe.hq.vendor.VendorProductRepository;
import org.example.stockitbe.hq.vendor.VendorRepository;
import org.example.stockitbe.hq.vendor.model.Vendor;
import org.example.stockitbe.hq.vendor.model.VendorProduct;
import org.example.stockitbe.hq.vendor.model.VendorProductStatus;
import org.example.stockitbe.hq.vendor.model.VendorStatus;
import org.example.stockitbe.user.model.AuthUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PurchaseOrderServiceTest {

    @Mock PurchaseOrderRepository purchaseOrderRepository;
    @Mock PurchaseOrderItemRepository itemRepository;
    @Mock PurchaseOrderStatusHistoryRepository historyRepository;
    @Mock VendorRepository vendorRepository;
    @Mock VendorProductRepository vendorProductRepository;
    @Mock InfrastructureRepository infrastructureRepository;
    @Mock ProductSkuRepository productSkuRepository;
    @Mock InventoryService inventoryService;

    @InjectMocks PurchaseOrderService service;

    private Vendor vendorA;
    private Vendor vendorB;
    private Vendor vendorC;
    private Infrastructure warehouse;
    private AuthUserDetails me;
    private AtomicLong idSeq;

    @BeforeEach
    void setUp() {
        vendorA = vendor(1L, "VND-A", "거래처A", "담당A");
        vendorB = vendor(2L, "VND-B", "거래처B", "담당B");
        vendorC = vendor(3L, "VND-C", "거래처C", "담당C");

        warehouse = Infrastructure.builder()
                .code("WH-CTR-0001")
                .name("중앙물류센터")
                .locationType(LocationType.WAREHOUSE)
                .build();
        ReflectionTestUtils.setField(warehouse, "id", 100L);

        me = mock(AuthUserDetails.class);
        when(me.getName()).thenReturn("이선엽");
        when(me.getLocationName()).thenReturn("본사");

        idSeq = new AtomicLong(1L);

        // save 시마다 id 박기 (실 DB 동작 시뮬레이션)
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> {
            PurchaseOrder po = inv.getArgument(0);
            ReflectionTestUtils.setField(po, "id", idSeq.getAndIncrement());
            return po;
        });

        // buildDetailRes 가 호출 — mock 환경에서 빈 list 반환 (DetailRes 의 items/history 검증은 본 테스트 범위 외)
        when(itemRepository.findAllByPurchaseOrderId(anyLong())).thenReturn(List.of());
        when(historyRepository.findAllByPurchaseOrderIdOrderByChangedAtAsc(anyLong())).thenReturn(List.of());

        // 창고 lookup
        when(infrastructureRepository.findByCode("WH-CTR-0001")).thenReturn(Optional.of(warehouse));

        // 코드 시퀀스 base = 1 (테스트 DB 가 비어있는 상태 가정)
        when(purchaseOrderRepository.countByCodeStartingWith(anyString())).thenReturn(0L);
    }

    private Vendor vendor(long id, String code, String name, String contactName) {
        Vendor v = Vendor.builder().code(code).name(name).contactName(contactName)
                .status(VendorStatus.ACTIVE).build();
        ReflectionTestUtils.setField(v, "id", id);
        return v;
    }

    private VendorProduct vp(long id, String code, Vendor vendor, String productCode, String productName, long unitPrice) {
        VendorProduct vp = VendorProduct.builder()
                .code(code).vendor(vendor).productCode(productCode).productName(productName)
                .unitPrice(unitPrice).status(VendorProductStatus.ACTIVE).build();
        ReflectionTestUtils.setField(vp, "id", id);
        return vp;
    }

    private ProductSku sku(String skuCode, String productCode, String color, String size, long unitPrice) {
        return ProductSku.builder()
                .skuCode(skuCode).productCode(productCode).color(color).size(size)
                .unitPrice(unitPrice).status(ProductStatus.ACTIVE).build();
    }

    private PurchaseOrderDto.ItemReq item(String vendorProductCode, String skuCode, int qty) {
        return PurchaseOrderDto.ItemReq.builder()
                .vendorProductCode(vendorProductCode).skuCode(skuCode).quantity(qty).build();
    }

    @Test
    @DisplayName("createBatch — 3개 vendor 분할 3건 생성 + 코드 연속 + vendorName ASC 정렬")
    void createBatch_3_vendor_분할_3건_생성() {
        // A: 2 SKU, B: 2 SKU, C: 1 SKU (입력 순서 A, B, C, B, A 섞어 그룹핑 검증)
        VendorProduct vpA = vp(11L, "VP-A-1", vendorA, "PM-A", "티셔츠", 6800L);
        VendorProduct vpB = vp(21L, "VP-B-1", vendorB, "PM-B", "니트", 15000L);
        VendorProduct vpC = vp(31L, "VP-C-1", vendorC, "PM-C", "자켓", 22000L);

        ProductSku skuA1 = sku("SKU-A-1", "PM-A", "화이트", "L", 6800L);
        ProductSku skuA2 = sku("SKU-A-2", "PM-A", "블랙", "M", 6800L);
        ProductSku skuB1 = sku("SKU-B-1", "PM-B", "네이비", "FREE", 15000L);
        ProductSku skuB2 = sku("SKU-B-2", "PM-B", "그레이", "FREE", 15000L);
        ProductSku skuC1 = sku("SKU-C-1", "PM-C", "카멜", "L", 22000L);

        when(vendorProductRepository.findByCode("VP-A-1")).thenReturn(Optional.of(vpA));
        when(vendorProductRepository.findByCode("VP-B-1")).thenReturn(Optional.of(vpB));
        when(vendorProductRepository.findByCode("VP-C-1")).thenReturn(Optional.of(vpC));
        when(productSkuRepository.findBySkuCode("SKU-A-1")).thenReturn(Optional.of(skuA1));
        when(productSkuRepository.findBySkuCode("SKU-A-2")).thenReturn(Optional.of(skuA2));
        when(productSkuRepository.findBySkuCode("SKU-B-1")).thenReturn(Optional.of(skuB1));
        when(productSkuRepository.findBySkuCode("SKU-B-2")).thenReturn(Optional.of(skuB2));
        when(productSkuRepository.findBySkuCode("SKU-C-1")).thenReturn(Optional.of(skuC1));

        // 입력 순서 의도적으로 섞음
        List<PurchaseOrderDto.ItemReq> items = new ArrayList<>();
        items.add(item("VP-A-1", "SKU-A-1", 2));   // A
        items.add(item("VP-B-1", "SKU-B-1", 1));   // B
        items.add(item("VP-C-1", "SKU-C-1", 3));   // C
        items.add(item("VP-B-1", "SKU-B-2", 4));   // B
        items.add(item("VP-A-1", "SKU-A-2", 5));   // A

        PurchaseOrderDto.BatchCreateReq req = PurchaseOrderDto.BatchCreateReq.builder()
                .warehouseCode("WH-CTR-0001")
                .memberId("EMP-001")
                .memberName("이선엽")
                .items(items)
                .build();

        PurchaseOrderDto.BatchCreateRes res = service.createBatch(req, me);

        // 메타 검증
        assertThat(res.getVendorCount()).isEqualTo(3);
        assertThat(res.getOrders()).hasSize(3);
        assertThat(res.getItemCount()).isEqualTo(5);
        // totalAmount = A(6800*2 + 6800*5) + B(15000*1 + 15000*4) + C(22000*3)
        long expectedTotal = 6800L * (2 + 5) + 15000L * (1 + 4) + 22000L * 3;
        assertThat(res.getTotalAmount()).isEqualTo(expectedTotal);

        // vendorName ASC 정렬: 거래처A, 거래처B, 거래처C
        assertThat(res.getOrders()).extracting(PurchaseOrderDto.DetailRes::getVendorName)
                .containsExactly("거래처A", "거래처B", "거래처C");

        // 코드 연속 — base seq = 1, 3그룹이므로 PO-YYYYMMDD-00001, 00002, 00003
        // 그룹 순서는 입력 첫 등장 순 (A, B, C), 정렬 후엔 vendorName ASC 으로 같은 순서
        assertThat(res.getOrders()).extracting(PurchaseOrderDto.DetailRes::getCode)
                .allMatch(code -> code.matches("PO-\\d{8}-\\d{5}"));
        List<String> seqSuffixes = res.getOrders().stream()
                .map(o -> o.getCode().substring(o.getCode().length() - 5))
                .toList();
        assertThat(seqSuffixes).containsExactly("00001", "00002", "00003");

        // save 가 3번 호출
        verify(purchaseOrderRepository, org.mockito.Mockito.times(3)).save(any(PurchaseOrder.class));
    }

    @Test
    @DisplayName("createBatch — 단일 vendor 라도 batch API 로 1건 생성 (vendorCount=1)")
    void createBatch_단일_vendor_1건_생성() {
        VendorProduct vpA = vp(11L, "VP-A-1", vendorA, "PM-A", "티셔츠", 6800L);
        ProductSku skuA1 = sku("SKU-A-1", "PM-A", "화이트", "L", 6800L);
        ProductSku skuA2 = sku("SKU-A-2", "PM-A", "블랙", "M", 6800L);
        ProductSku skuA3 = sku("SKU-A-3", "PM-A", "네이비", "S", 6800L);

        when(vendorProductRepository.findByCode("VP-A-1")).thenReturn(Optional.of(vpA));
        when(productSkuRepository.findBySkuCode("SKU-A-1")).thenReturn(Optional.of(skuA1));
        when(productSkuRepository.findBySkuCode("SKU-A-2")).thenReturn(Optional.of(skuA2));
        when(productSkuRepository.findBySkuCode("SKU-A-3")).thenReturn(Optional.of(skuA3));

        PurchaseOrderDto.BatchCreateReq req = PurchaseOrderDto.BatchCreateReq.builder()
                .warehouseCode("WH-CTR-0001")
                .memberId("EMP-001")
                .memberName("이선엽")
                .items(List.of(
                        item("VP-A-1", "SKU-A-1", 2),
                        item("VP-A-1", "SKU-A-2", 3),
                        item("VP-A-1", "SKU-A-3", 1)
                ))
                .build();

        PurchaseOrderDto.BatchCreateRes res = service.createBatch(req, me);

        assertThat(res.getVendorCount()).isEqualTo(1);
        assertThat(res.getOrders()).hasSize(1);
        assertThat(res.getItemCount()).isEqualTo(3);
        assertThat(res.getTotalAmount()).isEqualTo(6800L * (2 + 3 + 1));
        assertThat(res.getOrders().get(0).getVendorName()).isEqualTo("거래처A");
        assertThat(res.getOrders().get(0).getCode()).endsWith("-00001");

        verify(purchaseOrderRepository, org.mockito.Mockito.times(1)).save(any(PurchaseOrder.class));
    }

    @Test
    @DisplayName("createBatch — 멀티 vendor 카트에서 1건 SKU 없으면 전체 거절 (save 호출 0번)")
    void createBatch_잘못된_SKU_전체_롤백() {
        VendorProduct vpA = vp(11L, "VP-A-1", vendorA, "PM-A", "티셔츠", 6800L);
        VendorProduct vpB = vp(21L, "VP-B-1", vendorB, "PM-B", "니트", 15000L);
        ProductSku skuA1 = sku("SKU-A-1", "PM-A", "화이트", "L", 6800L);
        ProductSku skuB1 = sku("SKU-B-1", "PM-B", "네이비", "FREE", 15000L);
        // SKU-X-1 은 존재하지 않음

        when(vendorProductRepository.findByCode("VP-A-1")).thenReturn(Optional.of(vpA));
        when(vendorProductRepository.findByCode("VP-B-1")).thenReturn(Optional.of(vpB));
        when(productSkuRepository.findBySkuCode("SKU-A-1")).thenReturn(Optional.of(skuA1));
        when(productSkuRepository.findBySkuCode("SKU-B-1")).thenReturn(Optional.of(skuB1));
        when(productSkuRepository.findBySkuCode("SKU-X-1")).thenReturn(Optional.empty());

        PurchaseOrderDto.BatchCreateReq req = PurchaseOrderDto.BatchCreateReq.builder()
                .warehouseCode("WH-CTR-0001")
                .memberId("EMP-001")
                .memberName("이선엽")
                .items(List.of(
                        item("VP-A-1", "SKU-A-1", 2),
                        item("VP-B-1", "SKU-B-1", 1),
                        item("VP-A-1", "SKU-X-1", 3)   // invalid — lookup throw
                ))
                .build();

        assertThatThrownBy(() -> service.createBatch(req, me))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", BaseResponseStatus.PRODUCT_SKU_NOT_FOUND);

        // atomic — save 0번 (검증 단계 throw 라 saveAll 단계 도달 안 함)
        verify(purchaseOrderRepository, never()).save(any(PurchaseOrder.class));
    }

    @Test
    @DisplayName("createBatch — 빈 items 거절 (PURCHASE_ORDER_BATCH_EMPTY)")
    void createBatch_빈_items_거절() {
        PurchaseOrderDto.BatchCreateReq req = PurchaseOrderDto.BatchCreateReq.builder()
                .warehouseCode("WH-CTR-0001")
                .memberId("EMP-001")
                .memberName("이선엽")
                .items(List.of())
                .build();

        assertThatThrownBy(() -> service.createBatch(req, me))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("status", BaseResponseStatus.PURCHASE_ORDER_BATCH_EMPTY);

        verify(purchaseOrderRepository, never()).save(any(PurchaseOrder.class));
    }
}
