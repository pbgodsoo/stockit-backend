package org.example.stockitbe.hq.inventory;

import org.example.stockitbe.hq.category.CategoryRepository;
import org.example.stockitbe.hq.infrastructure.InfrastructureRepository;
import org.example.stockitbe.hq.infrastructure.model.Infrastructure;
import org.example.stockitbe.hq.infrastructure.model.LocationType;
import org.example.stockitbe.hq.inventory.model.Inventory;
import org.example.stockitbe.hq.inventory.model.InventoryDto;
import org.example.stockitbe.hq.inventory.model.InventoryStatus;
import org.example.stockitbe.hq.product.MaterialRepository;
import org.example.stockitbe.hq.product.ProductMasterRepository;
import org.example.stockitbe.hq.product.ProductSkuRepository;
import org.example.stockitbe.hq.product.model.ProductMaster;
import org.example.stockitbe.hq.product.model.ProductSku;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryServiceCircularCandidateTest {

    @Mock InventoryRepository inventoryRepository;
    @Mock InventoryCandidateConditionRepository inventoryCandidateConditionRepository;
    @Mock CircularMaterialPricePolicyRepository circularMaterialPricePolicyRepository;
    @Mock ProductSkuRepository productSkuRepository;
    @Mock ProductMasterRepository productMasterRepository;
    @Mock MaterialRepository materialRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock InfrastructureRepository infrastructureRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks InventoryService service;

    private Infrastructure warehouse;
    private AtomicLong inventoryIdSeq;

    @BeforeEach
    void setUp() {
        warehouse = Infrastructure.builder()
                .code("WH-001")
                .name("테스트 창고")
                .locationType(LocationType.WAREHOUSE)
                .build();
        ReflectionTestUtils.setField(warehouse, "id", 100L);
        when(infrastructureRepository.findAll()).thenReturn(List.of(warehouse));

        inventoryIdSeq = new AtomicLong(1_000L);
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> {
            Inventory inventory = invocation.getArgument(0);
            if (inventory.getId() == null) {
                ReflectionTestUtils.setField(inventory, "id", inventoryIdSeq.getAndIncrement());
            }
            return inventory;
        });
        when(inventoryCandidateConditionRepository.findAllByInventoryIdIn(anyCollection())).thenReturn(List.of());
    }

    @Test
    @DisplayName("조건2 — 안전재고 2.5배 초과분만 후보 row 로 분리한다")
    void refreshCircularCandidates_movesOnlyLowPerformanceExcess() {
        ProductSku sku = sku(1L, "SKU-1", "PRD-1", "BLACK", "L");
        ProductSku skuM = sku(2L, "SKU-2", "PRD-1", "WHITE", "M");
        ProductSku skuS = sku(3L, "SKU-3", "PRD-1", "GRAY", "S");
        ProductMaster master = master("PRD-1", 50);
        Inventory normal = inventory(10L, sku.getId(), 200, recentDate());
        Inventory normalM = inventory(20L, skuM.getId(), 100, recentDate());
        Inventory normalS = inventory(30L, skuS.getId(), 100, recentDate());

        when(inventoryRepository.findAllByInventoryStatus(InventoryStatus.NORMAL)).thenReturn(List.of(normal, normalM, normalS));
        when(productSkuRepository.findAllById(anyCollection())).thenReturn(List.of(sku, skuM, skuS));
        when(productMasterRepository.findAllByCodeIn(anyCollection())).thenReturn(List.of(master));
        when(inventoryRepository.findWithLockById(10L)).thenReturn(Optional.of(normal));
        when(inventoryRepository.findWithLockById(20L)).thenReturn(Optional.of(normalM));
        when(inventoryRepository.findWithLockById(30L)).thenReturn(Optional.of(normalS));
        when(inventoryRepository.findWithLockBySkuIdAndLocationIdAndInventoryStatus(
                eq(sku.getId()), eq(warehouse.getId()), eq(InventoryStatus.CIRCULAR_CANDIDATE)
        )).thenReturn(Optional.empty());
        when(inventoryRepository.findWithLockBySkuIdAndLocationIdAndInventoryStatus(
                eq(skuM.getId()), eq(warehouse.getId()), eq(InventoryStatus.CIRCULAR_CANDIDATE)
        )).thenReturn(Optional.empty());
        when(inventoryRepository.findWithLockBySkuIdAndLocationIdAndInventoryStatus(
                eq(skuS.getId()), eq(warehouse.getId()), eq(InventoryStatus.CIRCULAR_CANDIDATE)
        )).thenReturn(Optional.empty());

        InventoryDto.CircularCandidateRefreshRes result = service.refreshCircularCandidates();

        assertThat(result.getScannedCount()).isEqualTo(3);
        assertThat(result.getConvertedCount()).isEqualTo(1);
        assertThat(normal.getQuantity()).isEqualTo(125);
        assertThat(normal.getAvailableQuantity()).isEqualTo(125);

        Inventory candidate = savedCandidate();
        assertThat(candidate.getQuantity()).isEqualTo(75);
        assertThat(candidate.getAvailableQuantity()).isEqualTo(75);
    }

    @Test
    @DisplayName("조건3 — 60%를 넘는 사이즈 편중 초과분만 후보 row 로 분리한다")
    void refreshCircularCandidates_movesOnlySizeBiasExcess() {
        ProductSku skuL = sku(1L, "SKU-1", "PRD-1", "BLACK", "L");
        ProductSku skuM = sku(2L, "SKU-2", "PRD-1", "WHITE", "M");
        ProductMaster master = master("PRD-1", 1_000);
        Inventory normalL = inventory(10L, skuL.getId(), 80, recentDate());
        Inventory normalM = inventory(20L, skuM.getId(), 20, recentDate());

        when(inventoryRepository.findAllByInventoryStatus(InventoryStatus.NORMAL)).thenReturn(List.of(normalL, normalM));
        when(productSkuRepository.findAllById(anyCollection())).thenReturn(List.of(skuL, skuM));
        when(productMasterRepository.findAllByCodeIn(anyCollection())).thenReturn(List.of(master));
        when(inventoryRepository.findWithLockById(10L)).thenReturn(Optional.of(normalL));
        when(inventoryRepository.findWithLockById(20L)).thenReturn(Optional.of(normalM));
        when(inventoryRepository.findWithLockBySkuIdAndLocationIdAndInventoryStatus(
                eq(skuL.getId()), eq(warehouse.getId()), eq(InventoryStatus.CIRCULAR_CANDIDATE)
        )).thenReturn(Optional.empty());
        when(inventoryRepository.findWithLockBySkuIdAndLocationIdAndInventoryStatus(
                eq(skuM.getId()), eq(warehouse.getId()), eq(InventoryStatus.CIRCULAR_CANDIDATE)
        )).thenReturn(Optional.empty());

        InventoryDto.CircularCandidateRefreshRes result = service.refreshCircularCandidates();

        assertThat(result.getScannedCount()).isEqualTo(2);
        assertThat(result.getConvertedCount()).isEqualTo(1);
        assertThat(normalL.getQuantity()).isEqualTo(30);
        assertThat(normalL.getAvailableQuantity()).isEqualTo(30);
        assertThat(normalM.getQuantity()).isEqualTo(20);
        assertThat(normalM.getAvailableQuantity()).isEqualTo(20);

        Inventory candidate = savedCandidate();
        assertThat(candidate.getSkuId()).isEqualTo(skuL.getId());
        assertThat(candidate.getQuantity()).isEqualTo(50);
        assertThat(candidate.getAvailableQuantity()).isEqualTo(50);
    }

    private Inventory savedCandidate() {
        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        org.mockito.Mockito.verify(inventoryRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getAllValues().stream()
                .filter(inv -> inv.getInventoryStatus() == InventoryStatus.CIRCULAR_CANDIDATE)
                .reduce((first, second) -> second)
                .orElseThrow();
    }

    private Inventory inventory(Long id, Long skuId, int quantity, Date lastMovementAt) {
        Inventory inventory = Inventory.builder()
                .skuId(skuId)
                .locationId(warehouse.getId())
                .inventoryStatus(InventoryStatus.NORMAL)
                .quantity(quantity)
                .availableQuantity(quantity)
                .reservedQuantity(0)
                .inTransitQuantity(0)
                .statusChangedAt(recentDate())
                .lastMovementAt(lastMovementAt)
                .build();
        ReflectionTestUtils.setField(inventory, "id", id);
        return inventory;
    }

    private ProductSku sku(Long id, String skuCode, String productCode, String color, String size) {
        ProductSku sku = ProductSku.builder()
                .skuCode(skuCode)
                .productCode(productCode)
                .color(color)
                .size(size)
                .unitPrice(10_000L)
                .build();
        ReflectionTestUtils.setField(sku, "id", id);
        return sku;
    }

    private ProductMaster master(String code, int warehouseSafetyStock) {
        return ProductMaster.builder()
                .code(code)
                .name("테스트 상품")
                .categoryCode("CAT-001")
                .basePrice(10_000L)
                .leadTimeDays(3)
                .warehouseSafetyStock(warehouseSafetyStock)
                .storeSafetyStock(10)
                .mainVendorCode("VND-001")
                .build();
    }

    private Date recentDate() {
        return new Date(System.currentTimeMillis() - (10L * 24 * 60 * 60 * 1000));
    }
}
