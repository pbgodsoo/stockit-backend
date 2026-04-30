package org.example.stockitbe.hq.purchaseorder;

import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.hq.product.ProductMasterRepository;
import org.example.stockitbe.hq.product.ProductSkuRepository;
import org.example.stockitbe.hq.product.model.ProductMaster;
import org.example.stockitbe.hq.product.model.ProductSku;
import org.example.stockitbe.hq.product.model.ProductStatus;
import org.example.stockitbe.hq.purchaseorder.model.PurchaseOrderCatalogDto;
import org.example.stockitbe.hq.vendor.VendorProductRepository;
import org.example.stockitbe.hq.vendor.VendorRepository;
import org.example.stockitbe.hq.vendor.model.Vendor;
import org.example.stockitbe.hq.vendor.model.VendorProduct;
import org.example.stockitbe.hq.vendor.model.VendorProductStatus;
import org.example.stockitbe.hq.vendor.model.VendorStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderCatalogServiceTest {

    @Mock VendorProductRepository vendorProductRepository;
    @Mock VendorRepository vendorRepository;
    @Mock ProductMasterRepository productMasterRepository;
    @Mock ProductSkuRepository productSkuRepository;

    @InjectMocks PurchaseOrderCatalogService service;

    private Vendor vendorA;
    private Vendor vendorB;
    private VendorProduct vpA1; // vendorA, productCode=PM-A
    private VendorProduct vpB1; // vendorB, productCode=PM-B
    private VendorProduct vpA2; // vendorA, productCode=PM-EMPTY (SKU 0건)
    private ProductMaster pmA;
    private ProductMaster pmB;
    private ProductMaster pmEmpty;

    @BeforeEach
    void setUp() {
        vendorA = Vendor.builder().code("VND-A").name("거래처A").contactName("담당A")
                .status(VendorStatus.ACTIVE).build();
        ReflectionTestUtils.setField(vendorA, "id", 1L);

        vendorB = Vendor.builder().code("VND-B").name("거래처B").contactName("담당B")
                .status(VendorStatus.ACTIVE).build();
        ReflectionTestUtils.setField(vendorB, "id", 2L);

        vpA1 = VendorProduct.builder().code("VP-A-001").vendorId(1L)
                .productCode("PM-A").productName("티셔츠").unitPrice(6800L)
                .status(VendorProductStatus.ACTIVE).build();
        ReflectionTestUtils.setField(vpA1, "id", 11L);

        vpB1 = VendorProduct.builder().code("VP-B-001").vendorId(2L)
                .productCode("PM-B").productName("니트").unitPrice(15000L)
                .status(VendorProductStatus.ACTIVE).build();
        ReflectionTestUtils.setField(vpB1, "id", 21L);

        vpA2 = VendorProduct.builder().code("VP-A-002").vendorId(1L)
                .productCode("PM-EMPTY").productName("빈제품").unitPrice(9000L)
                .status(VendorProductStatus.ACTIVE).build();
        ReflectionTestUtils.setField(vpA2, "id", 12L);

        pmA = ProductMaster.builder().code("PM-A").name("티셔츠").categoryCode("CAT-1")
                .basePrice(6800L).leadTimeDays(7).mainVendorCode("VND-A")
                .status(ProductStatus.ACTIVE).build();
        pmB = ProductMaster.builder().code("PM-B").name("니트").categoryCode("CAT-1")
                .basePrice(15000L).leadTimeDays(10).mainVendorCode("VND-B")
                .status(ProductStatus.ACTIVE).build();
        pmEmpty = ProductMaster.builder().code("PM-EMPTY").name("빈제품").categoryCode("CAT-1")
                .basePrice(9000L).leadTimeDays(7).mainVendorCode("VND-A")
                .status(ProductStatus.ACTIVE).build();
    }

    private ProductSku sku(String skuCode, String productCode, String optionName, String optionValue, long price) {
        return ProductSku.builder()
                .skuCode(skuCode)
                .productCode(productCode)
                .optionName(optionName)
                .optionValue(optionValue)
                .unitPrice(price)
                .status(ProductStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("vendorCode 미지정 — 모든 ACTIVE 거래처 펼침 + optionFacets axes 통합")
    void getCatalog_allVendors() {
        when(vendorProductRepository.findAllByStatusOrderByIdDesc(VendorProductStatus.ACTIVE))
                .thenReturn(List.of(vpA1, vpB1));
        when(vendorRepository.findAllById(any())).thenReturn(List.of(vendorA, vendorB));
        when(productMasterRepository.findAllByCodeIn(any())).thenReturn(List.of(pmA, pmB));
        when(productSkuRepository.findAllByProductCodeInOrderByIdAsc(any())).thenReturn(List.of(
                sku("SKU-A-1", "PM-A", "색상/사이즈", "화이트/L", 6800L),
                sku("SKU-A-2", "PM-A", "색상/사이즈", "블랙/M", 7200L),
                sku("SKU-B-1", "PM-B", "색상", "네이비", 15000L)
        ));

        PurchaseOrderCatalogDto.CatalogRes res = service.getCatalog(null, null);

        assertThat(res.getMasters()).hasSize(2);
        PurchaseOrderCatalogDto.MasterRes masterA = res.getMasters().stream()
                .filter(m -> m.getVendorProductCode().equals("VP-A-001")).findFirst().orElseThrow();
        assertThat(masterA.getSkus()).hasSize(2);
        assertThat(masterA.getMinSkuUnitPrice()).isEqualTo(6800L);
        assertThat(masterA.getMaxSkuUnitPrice()).isEqualTo(7200L);
        assertThat(masterA.getVendorName()).isEqualTo("거래처A");

        // optionFacets — 색상/사이즈 axes 분리, 색상 axis 한 곳에 통합
        assertThat(res.getOptionFacets()).extracting(PurchaseOrderCatalogDto.FacetRes::getName)
                .containsExactlyInAnyOrder("색상", "사이즈");
        PurchaseOrderCatalogDto.FacetRes color = res.getOptionFacets().stream()
                .filter(f -> f.getName().equals("색상")).findFirst().orElseThrow();
        assertThat(color.getValues()).contains("화이트", "블랙", "네이비");
    }

    @Test
    @DisplayName("SKU 0건 마스터는 결과에서 제외")
    void getCatalog_skipMastersWithoutSkus() {
        when(vendorProductRepository.findAllByStatusOrderByIdDesc(VendorProductStatus.ACTIVE))
                .thenReturn(List.of(vpA1, vpA2));
        when(vendorRepository.findAllById(any())).thenReturn(List.of(vendorA));
        when(productMasterRepository.findAllByCodeIn(any())).thenReturn(List.of(pmA, pmEmpty));
        when(productSkuRepository.findAllByProductCodeInOrderByIdAsc(any())).thenReturn(List.of(
                sku("SKU-A-1", "PM-A", "사이즈", "L", 6800L)
                // PM-EMPTY 의 SKU 는 0건
        ));

        PurchaseOrderCatalogDto.CatalogRes res = service.getCatalog(null, null);

        assertThat(res.getMasters()).hasSize(1);
        assertThat(res.getMasters().get(0).getProductCode()).isEqualTo("PM-A");
    }

    @Test
    @DisplayName("vendorCode 지정 — 다른 거래처 결과 안 섞임")
    void getCatalog_filterByVendor() {
        when(vendorRepository.findByCode("VND-A")).thenReturn(java.util.Optional.of(vendorA));
        when(vendorProductRepository.findAllByVendorIdAndStatusNotOrderByIdDesc(1L, VendorProductStatus.DELETED))
                .thenReturn(List.of(vpA1));
        when(vendorRepository.findAllById(any())).thenReturn(List.of(vendorA));
        when(productMasterRepository.findAllByCodeIn(any())).thenReturn(List.of(pmA));
        when(productSkuRepository.findAllByProductCodeInOrderByIdAsc(any())).thenReturn(List.of(
                sku("SKU-A-1", "PM-A", "사이즈", "L", 6800L)
        ));

        PurchaseOrderCatalogDto.CatalogRes res = service.getCatalog("VND-A", null);

        assertThat(res.getMasters()).hasSize(1);
        assertThat(res.getMasters().get(0).getVendorCode()).isEqualTo("VND-A");
    }

    @Test
    @DisplayName("vendorCode 미존재 시 VENDOR_NOT_FOUND throw")
    void getCatalog_vendorNotFound() {
        when(vendorRepository.findByCode("VND-XYZ")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.getCatalog("VND-XYZ", null))
                .isInstanceOf(BaseException.class);
    }
}
