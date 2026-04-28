package org.example.stockitbe.hq.product;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.category.CategoryRepository;
import org.example.stockitbe.hq.product.model.ProductDto;
import org.example.stockitbe.hq.product.model.ProductMaster;
import org.example.stockitbe.hq.product.model.ProductSku;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductMasterService {

    private final ProductMasterRepository productMasterRepository;
    private final ProductSkuRepository productSkuRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<ProductDto.ProductMasterRes> findProducts(String keyword, String categoryCode) {
        String safeKeyword = keyword == null ? "" : keyword.trim();
        String safeCategoryCode = categoryCode == null ? "" : categoryCode.trim();
        return productMasterRepository
                .findByNameContainingIgnoreCaseAndCategoryCodeContainingIgnoreCaseOrderByIdDesc(safeKeyword, safeCategoryCode)
                .stream()
                .map(p -> ProductDto.ProductMasterRes.from(p, productSkuRepository.countByProductCode(p.getCode())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductDto.ProductMasterRes findProductByCode(String code) {
        ProductMaster product = productMasterRepository.findByCode(code)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.PRODUCT_MASTER_NOT_FOUND));
        return ProductDto.ProductMasterRes.from(product, productSkuRepository.countByProductCode(product.getCode()));
    }

    @Transactional
    public ProductDto.ProductMasterRes createProduct(ProductDto.ProductMasterUpsertReq req) {
        validateCreate(req);
        ProductMaster saved = saveWithGeneratedCode(req);
        return ProductDto.ProductMasterRes.from(saved, 0);
    }

    @Transactional
    public ProductDto.ProductMasterRes updateProduct(String code, ProductDto.ProductMasterUpsertReq req) {
        ProductMaster product = productMasterRepository.findByCode(code)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.PRODUCT_MASTER_NOT_FOUND));
        validateUpdate(req, code);
        product.update(req.getName().trim(), req.getCategoryCode().trim(), req.getBasePrice(), req.getLeadTimeDays(), req.getMainVendorCode().trim(), req.getStatus());
        return ProductDto.ProductMasterRes.from(product, productSkuRepository.countByProductCode(product.getCode()));
    }

    @Transactional
    public void deleteProduct(String code) {
        ProductMaster product = productMasterRepository.findByCode(code)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.PRODUCT_MASTER_NOT_FOUND));
        productSkuRepository.deleteByProductCode(product.getCode());
        productMasterRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public List<ProductDto.ProductSkuRes> findSkus(String productCode) {
        productMasterRepository.findByCode(productCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.PRODUCT_MASTER_NOT_FOUND));
        return productSkuRepository.findByProductCodeOrderByIdDesc(productCode)
                .stream().map(ProductDto.ProductSkuRes::from).toList();
    }

    @Transactional
    public ProductDto.ProductSkuRes createSku(String productCode, ProductDto.ProductSkuUpsertReq req) {
        productMasterRepository.findByCode(productCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.PRODUCT_MASTER_NOT_FOUND));
        validateSkuDuplicate(productCode, req.getOptionName().trim(), req.getOptionValue().trim(), null);
        ProductSku saved = saveSkuWithGeneratedCode(productCode, req);
        return ProductDto.ProductSkuRes.from(saved);
    }

    @Transactional
    public ProductDto.ProductSkuRes updateSku(String skuCode, ProductDto.ProductSkuUpsertReq req) {
        ProductSku sku = productSkuRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.PRODUCT_SKU_NOT_FOUND));
        String optionName = req.getOptionName().trim();
        String optionValue = req.getOptionValue().trim();
        validateSkuDuplicate(sku.getProductCode(), optionName, optionValue, skuCode);
        sku.update(optionName, optionValue, req.getUnitPrice(), req.getStatus());
        return ProductDto.ProductSkuRes.from(sku);
    }

    @Transactional
    public void deleteSku(String skuCode) {
        ProductSku sku = productSkuRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.PRODUCT_SKU_NOT_FOUND));
        productSkuRepository.delete(sku);
    }

    @Transactional
    public ProductDto.ProductSkuBulkCreateRes bulkCreateSkus(String productCode, ProductDto.ProductSkuBulkCreateReq req) {
        productMasterRepository.findByCode(productCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.PRODUCT_MASTER_NOT_FOUND));

        String optionName = req.getOptionName().trim();
        Set<String> uniqueValues = new LinkedHashSet<>();
        for (String raw : req.getOptionValues()) {
            if (raw == null) continue;
            String trimmed = raw.trim();
            if (!trimmed.isEmpty()) uniqueValues.add(trimmed);
        }

        int requestedCount = uniqueValues.size();
        int createdCount = 0;
        int skippedCount = 0;

        for (String optionValue : uniqueValues) {
            boolean duplicated = productSkuRepository.existsByProductCodeAndOptionNameAndOptionValue(productCode, optionName, optionValue);
            if (duplicated) {
                skippedCount++;
                continue;
            }
            ProductDto.ProductSkuUpsertReq createReq = ProductDto.ProductSkuUpsertReq.builder()
                    .optionName(optionName)
                    .optionValue(optionValue)
                    .unitPrice(req.getUnitPrice())
                    .status(req.getStatus())
                    .build();
            saveSkuWithGeneratedCode(productCode, createReq);
            createdCount++;
        }

        return ProductDto.ProductSkuBulkCreateRes.builder()
                .productCode(productCode)
                .requestedCount(requestedCount)
                .createdCount(createdCount)
                .skippedCount(skippedCount)
                .build();
    }

    @Transactional
    public ProductDto.ProductSkuPriceBulkUpdateRes updateAllSkuPrices(String productCode, ProductDto.ProductSkuPriceBulkUpdateReq req) {
        productMasterRepository.findByCode(productCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.PRODUCT_MASTER_NOT_FOUND));

        if (req.getUnitPrice() == null || req.getUnitPrice() < 0) {
            throw BaseException.from(BaseResponseStatus.INVALID_SKU_PRICE);
        }

        List<ProductSku> targetSkus = productSkuRepository.findByProductCodeOrderByIdDesc(productCode);
        for (ProductSku sku : targetSkus) {
            sku.update(sku.getOptionName(), sku.getOptionValue(), req.getUnitPrice(), sku.getStatus());
        }

        return ProductDto.ProductSkuPriceBulkUpdateRes.builder()
                .productCode(productCode)
                .updatedCount(targetSkus.size())
                .unitPrice(req.getUnitPrice())
                .build();
    }

    @Transactional
    public ProductDto.ProductSkuStatusBulkUpdateRes updateAllSkuStatus(String productCode, ProductDto.ProductSkuStatusBulkUpdateReq req) {
        productMasterRepository.findByCode(productCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.PRODUCT_MASTER_NOT_FOUND));

        List<ProductSku> targetSkus = productSkuRepository.findByProductCodeOrderByIdDesc(productCode);
        for (ProductSku sku : targetSkus) {
            sku.update(sku.getOptionName(), sku.getOptionValue(), sku.getUnitPrice(), req.getStatus());
        }

        return ProductDto.ProductSkuStatusBulkUpdateRes.builder()
                .productCode(productCode)
                .updatedCount(targetSkus.size())
                .status(req.getStatus())
                .build();
    }

    private void validateCreate(ProductDto.ProductMasterUpsertReq req) {
        if (!categoryRepository.findByCode(req.getCategoryCode().trim()).isPresent()) {
            throw BaseException.from(BaseResponseStatus.CATEGORY_NOT_FOUND);
        }
        if (productMasterRepository.existsByNameIgnoreCase(req.getName().trim())) {
            throw BaseException.from(BaseResponseStatus.DUPLICATE_PRODUCT_MASTER_NAME);
        }
    }

    private void validateUpdate(ProductDto.ProductMasterUpsertReq req, String code) {
        if (!categoryRepository.findByCode(req.getCategoryCode().trim()).isPresent()) {
            throw BaseException.from(BaseResponseStatus.CATEGORY_NOT_FOUND);
        }
        if (productMasterRepository.existsByNameIgnoreCaseAndCodeNot(req.getName().trim(), code)) {
            throw BaseException.from(BaseResponseStatus.DUPLICATE_PRODUCT_MASTER_NAME);
        }
    }

    private ProductMaster saveWithGeneratedCode(ProductDto.ProductMasterUpsertReq req) {
        for (int i = 0; i < 2; i++) {
            String code = nextCode(productMasterRepository.findAllByOrderByIdDesc().stream().map(ProductMaster::getCode).toList(), "PM");
            try {
                return productMasterRepository.save(req.toEntity(code));
            } catch (DataIntegrityViolationException e) {
                if (i == 1) throw e;
            }
        }
        throw BaseException.from(BaseResponseStatus.FAIL);
    }

    private ProductSku saveSkuWithGeneratedCode(String productCode, ProductDto.ProductSkuUpsertReq req) {
        for (int i = 0; i < 2; i++) {
            String code = nextCode(productSkuRepository.findAllByOrderByIdDesc().stream().map(ProductSku::getSkuCode).toList(), "SKU");
            try {
                return productSkuRepository.save(req.toEntity(code, productCode));
            } catch (DataIntegrityViolationException e) {
                if (i == 1) throw e;
            }
        }
        throw BaseException.from(BaseResponseStatus.FAIL);
    }

    private void validateSkuDuplicate(String productCode, String optionName, String optionValue, String selfSkuCode) {
        List<ProductSku> sameProductSkus = productSkuRepository.findByProductCodeOrderByIdDesc(productCode);
        boolean duplicated = sameProductSkus.stream()
                .anyMatch(s -> s.getOptionName().equalsIgnoreCase(optionName)
                        && s.getOptionValue().equalsIgnoreCase(optionValue)
                        && (selfSkuCode == null || !s.getSkuCode().equals(selfSkuCode)));
        if (duplicated) {
            throw BaseException.from(BaseResponseStatus.DUPLICATE_PRODUCT_SKU_OPTION);
        }
    }

    private String nextCode(List<String> codes, String prefix) {
        long max = codes.stream()
                .filter(c -> c != null && c.startsWith(prefix + "-"))
                .mapToLong(c -> {
                    try {
                        return Long.parseLong(c.substring(prefix.length() + 1));
                    } catch (Exception e) {
                        return 0L;
                    }
                })
                .max().orElse(0L);
        return String.format("%s-%04d", prefix, max + 1);
    }
}
