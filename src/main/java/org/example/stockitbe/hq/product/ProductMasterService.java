package org.example.stockitbe.hq.product;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.category.CategoryRepository;
import org.example.stockitbe.hq.product.model.Material;
import org.example.stockitbe.hq.product.model.ProductDto;
import org.example.stockitbe.hq.product.model.ProductMaster;
import org.example.stockitbe.hq.product.model.ProductMaterialComposition;
import org.example.stockitbe.hq.product.model.ProductMaterialType;
import org.example.stockitbe.hq.product.model.ProductSku;
import org.example.stockitbe.hq.vendor.VendorRepository;
import org.example.stockitbe.hq.vendor.model.Vendor;
import org.example.stockitbe.hq.vendor.model.VendorStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductMasterService {

    private static final String MATERIAL_GROUP_NATURAL = "NATURAL";
    private static final String MATERIAL_GROUP_SYNTHETIC = "SYNTHETIC";

    private static final Set<String> ALLOWED_COLORS = Set.of("BLK", "WHT", "NVY", "GRY");
    private static final Set<String> ALLOWED_SIZES = Set.of("XS", "S", "M", "L", "XL");
    private static final Pattern CATEGORY_CODE_PATTERN = Pattern.compile("^CAT-L2-([A-Z]{3})-([A-Z]{2})$");
    private static final Pattern PRODUCT_CODE_PATTERN = Pattern.compile("^PRD-([A-Z]{3})-([A-Z]{2})-(\\d{3})$");

    private final ProductMasterRepository productMasterRepository;
    private final ProductSkuRepository productSkuRepository;
    private final MaterialRepository materialRepository;
    private final CategoryRepository categoryRepository;
    private final VendorRepository vendorRepository;

    @Transactional(readOnly = true)
    public List<ProductDto.ProductMasterRes> findProducts(String keyword, String categoryCode) {
        String safeKeyword = keyword == null ? "" : keyword.trim();
        String safeCategoryCode = categoryCode == null ? "" : categoryCode.trim();
        Map<String, String> materialNameMap = loadMaterialNameMap();

        return productMasterRepository
                .findByNameContainingIgnoreCaseAndCategoryCodeContainingIgnoreCaseOrderByIdDesc(safeKeyword, safeCategoryCode)
                .stream()
                .map(p -> ProductDto.ProductMasterRes.from(
                        p,
                        productSkuRepository.countByProductCode(p.getCode()),
                        deriveMaterialType(p.getMaterialCompositions()),
                        materialNameMap
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductDto.ProductMasterRes findProductByCode(String code) {
        ProductMaster product = productMasterRepository.findByCode(code)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.PRODUCT_MASTER_NOT_FOUND));
        return ProductDto.ProductMasterRes.from(
                product,
                productSkuRepository.countByProductCode(product.getCode()),
                deriveMaterialType(product.getMaterialCompositions()),
                loadMaterialNameMap()
        );
    }

    @Transactional
    public ProductDto.ProductMasterRes createProduct(ProductDto.ProductMasterUpsertReq req) {
        validateCreate(req);
        ProductMaster saved = saveWithGeneratedCode(req);
        List<ProductMaterialComposition> compositions = buildCompositions(req);
        saved.replaceMaterialCompositions(compositions);

        return ProductDto.ProductMasterRes.from(
                saved,
                0,
                deriveMaterialType(saved.getMaterialCompositions()),
                loadMaterialNameMap()
        );
    }

    @Transactional
    public ProductDto.ProductMasterRes updateProduct(String code, ProductDto.ProductMasterUpsertReq req) {
        ProductMaster product = productMasterRepository.findByCode(code)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.PRODUCT_MASTER_NOT_FOUND));
        validateUpdate(req, code);

        product.update(
                req.getName().trim(),
                req.getCategoryCode().trim(),
                req.getBasePrice(),
                req.getLeadTimeDays(),
                req.getWarehouseSafetyStock(),
                req.getStoreSafetyStock(),
                req.getMainVendorCode().trim(),
                req.getStatus()
        );
        product.replaceMaterialCompositions(buildCompositions(req));

        return ProductDto.ProductMasterRes.from(
                product,
                productSkuRepository.countByProductCode(product.getCode()),
                deriveMaterialType(product.getMaterialCompositions()),
                loadMaterialNameMap()
        );
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
        String color = normalizeAndValidateColor(req.getColor());
        String size = normalizeAndValidateSize(req.getSize());
        validateSkuDuplicate(productCode, color, size, null);
        ProductSku saved = saveSkuWithGeneratedCode(productCode, req, color, size);
        return ProductDto.ProductSkuRes.from(saved);
    }

    @Transactional
    public ProductDto.ProductSkuRes updateSku(String skuCode, ProductDto.ProductSkuUpsertReq req) {
        ProductSku sku = productSkuRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.PRODUCT_SKU_NOT_FOUND));
        String color = normalizeAndValidateColor(req.getColor());
        String size = normalizeAndValidateSize(req.getSize());
        validateSkuDuplicate(sku.getProductCode(), color, size, skuCode);
        sku.update(color, size, req.getUnitPrice(), req.getStatus());
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

        Set<String> colorSet = req.getColors().stream()
                .filter(v -> v != null && !v.trim().isEmpty())
                .map(this::normalizeAndValidateColor)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> sizeSet = req.getSizes().stream()
                .filter(v -> v != null && !v.trim().isEmpty())
                .map(this::normalizeAndValidateSize)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        int requestedCount = colorSet.size() * sizeSet.size();
        int createdCount = 0;
        int skippedCount = 0;

        for (String color : colorSet) {
            for (String size : sizeSet) {
                boolean duplicated = productSkuRepository.existsByProductCodeAndColorAndSize(productCode, color, size);
                if (duplicated) {
                    skippedCount++;
                    continue;
                }
                ProductDto.ProductSkuUpsertReq createReq = ProductDto.ProductSkuUpsertReq.builder()
                        .color(color)
                        .size(size)
                        .unitPrice(req.getUnitPrice())
                        .status(req.getStatus())
                        .build();
                saveSkuWithGeneratedCode(productCode, createReq, color, size);
                createdCount++;
            }
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
            sku.update(sku.getColor(), sku.getSize(), req.getUnitPrice(), sku.getStatus());
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
            sku.update(sku.getColor(), sku.getSize(), sku.getUnitPrice(), req.getStatus());
        }

        return ProductDto.ProductSkuStatusBulkUpdateRes.builder()
                .productCode(productCode)
                .updatedCount(targetSkus.size())
                .status(req.getStatus())
                .build();
    }

    private void validateCreate(ProductDto.ProductMasterUpsertReq req) {
        String categoryCode = req.getCategoryCode().trim();
        if (!categoryRepository.findByCode(categoryCode).isPresent()) {
            throw BaseException.from(BaseResponseStatus.CATEGORY_NOT_FOUND);
        }
        if (!CATEGORY_CODE_PATTERN.matcher(categoryCode).matches()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
        validateMainVendor(req.getMainVendorCode());
        validateMaterialComposition(req);
        if (productMasterRepository.existsByNameIgnoreCase(req.getName().trim())) {
            throw BaseException.from(BaseResponseStatus.DUPLICATE_PRODUCT_MASTER_NAME);
        }
    }

    private void validateUpdate(ProductDto.ProductMasterUpsertReq req, String code) {
        String categoryCode = req.getCategoryCode().trim();
        if (!categoryRepository.findByCode(categoryCode).isPresent()) {
            throw BaseException.from(BaseResponseStatus.CATEGORY_NOT_FOUND);
        }
        if (!CATEGORY_CODE_PATTERN.matcher(categoryCode).matches()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
        validateMainVendor(req.getMainVendorCode());
        validateMaterialComposition(req);
        if (productMasterRepository.existsByNameIgnoreCaseAndCodeNot(req.getName().trim(), code)) {
            throw BaseException.from(BaseResponseStatus.DUPLICATE_PRODUCT_MASTER_NAME);
        }
    }

    private void validateMaterialComposition(ProductDto.ProductMasterUpsertReq req) {
        List<ProductDto.ProductMaterialCompositionReq> compositions = req.getMaterialCompositions();
        if (req.getMaterialType() == null || compositions == null || compositions.isEmpty()) {
            throw BaseException.from(BaseResponseStatus.INVALID_PRODUCT_MATERIAL_SPEC);
        }

        Set<String> codes = new LinkedHashSet<>();
        int ratioSum = 0;
        for (ProductDto.ProductMaterialCompositionReq composition : compositions) {
            if (composition == null || composition.getMaterialCode() == null || composition.getMaterialCode().trim().isEmpty()) {
                throw BaseException.from(BaseResponseStatus.INVALID_PRODUCT_MATERIAL_SPEC);
            }
            String code = composition.getMaterialCode().trim().toUpperCase(Locale.ROOT);
            Integer ratio = composition.getRatio();
            if (ratio == null || ratio <= 0 || !codes.add(code)) {
                throw BaseException.from(BaseResponseStatus.INVALID_PRODUCT_MATERIAL_SPEC);
            }
            ratioSum += ratio;
        }

        if (ratioSum != 100) {
            throw BaseException.from(BaseResponseStatus.INVALID_PRODUCT_MATERIAL_SPEC);
        }

        Map<String, Material> materialByCode = materialRepository.findAllByCodeIn(codes).stream()
                .collect(Collectors.toMap(Material::getCode, m -> m));
        if (materialByCode.size() != codes.size()) {
            throw BaseException.from(BaseResponseStatus.INVALID_PRODUCT_MATERIAL_SPEC);
        }

        if (req.getMaterialType() == ProductMaterialType.BLEND) {
            if (compositions.size() < 2) {
                throw BaseException.from(BaseResponseStatus.INVALID_PRODUCT_MATERIAL_SPEC);
            }
            return;
        }

        if (compositions.size() != 1) {
            throw BaseException.from(BaseResponseStatus.INVALID_PRODUCT_MATERIAL_SPEC);
        }

        String singleCode = compositions.get(0).getMaterialCode().trim().toUpperCase(Locale.ROOT);
        Material single = materialByCode.get(singleCode);
        if (single == null || single.getMaterialGroup() == null) {
            throw BaseException.from(BaseResponseStatus.INVALID_PRODUCT_MATERIAL_SPEC);
        }
        String group = single.getMaterialGroup().trim().toUpperCase(Locale.ROOT);
        if (req.getMaterialType() == ProductMaterialType.NATURAL_SINGLE && !MATERIAL_GROUP_NATURAL.equals(group)) {
            throw BaseException.from(BaseResponseStatus.INVALID_PRODUCT_MATERIAL_SPEC);
        }
        if (req.getMaterialType() == ProductMaterialType.SYNTHETIC && !MATERIAL_GROUP_SYNTHETIC.equals(group)) {
            throw BaseException.from(BaseResponseStatus.INVALID_PRODUCT_MATERIAL_SPEC);
        }
    }

    private List<ProductMaterialComposition> buildCompositions(ProductDto.ProductMasterUpsertReq req) {
        List<ProductDto.ProductMaterialCompositionReq> requested = req.getMaterialCompositions();
        List<String> codes = requested.stream()
                .map(c -> c.getMaterialCode().trim().toUpperCase(Locale.ROOT))
                .toList();

        Map<String, Material> materialByCode = materialRepository.findAllByCodeIn(codes).stream()
                .collect(Collectors.toMap(Material::getCode, m -> m));

        List<ProductMaterialComposition> rows = new ArrayList<>();
        for (int i = 0; i < requested.size(); i++) {
            ProductDto.ProductMaterialCompositionReq item = requested.get(i);
            String code = item.getMaterialCode().trim().toUpperCase(Locale.ROOT);
            Material material = materialByCode.get(code);
            if (material == null) {
                throw BaseException.from(BaseResponseStatus.INVALID_PRODUCT_MATERIAL_SPEC);
            }
            rows.add(new ProductMaterialComposition(material, item.getRatio(), i + 1));
        }
        return rows;
    }

    private ProductMaterialType deriveMaterialType(List<ProductMaterialComposition> compositions) {
        if (compositions == null || compositions.isEmpty()) {
            return ProductMaterialType.BLEND;
        }
        if (compositions.size() >= 2) {
            return ProductMaterialType.BLEND;
        }

        Material material = compositions.get(0).getMaterial();
        if (material == null || material.getMaterialGroup() == null) {
            return ProductMaterialType.BLEND;
        }
        String group = material.getMaterialGroup().trim().toUpperCase(Locale.ROOT);
        if (MATERIAL_GROUP_NATURAL.equals(group)) {
            return ProductMaterialType.NATURAL_SINGLE;
        }
        if (MATERIAL_GROUP_SYNTHETIC.equals(group)) {
            return ProductMaterialType.SYNTHETIC;
        }
        return ProductMaterialType.BLEND;
    }

    private Map<String, String> loadMaterialNameMap() {
        return materialRepository.findAllByActiveTrueOrderByCodeAsc().stream()
                .collect(Collectors.toMap(Material::getCode, Material::getNameKo));
    }

    private void validateMainVendor(String mainVendorCode) {
        Vendor vendor = vendorRepository.findByCode(mainVendorCode.trim())
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.VENDOR_NOT_FOUND));
        if (vendor.getStatus() != VendorStatus.ACTIVE) {
            throw BaseException.from(BaseResponseStatus.VENDOR_INACTIVE);
        }
    }

    private ProductMaster saveWithGeneratedCode(ProductDto.ProductMasterUpsertReq req) {
        String categoryCode = req.getCategoryCode().trim();
        Matcher matcher = CATEGORY_CODE_PATTERN.matcher(categoryCode);
        if (!matcher.matches()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
        String l1 = matcher.group(1);
        String l2 = matcher.group(2);
        String codePrefix = String.format("PRD-%s-%s", l1, l2);

        for (int i = 0; i < 2; i++) {
            String code = nextProductCode(codePrefix);
            try {
                return productMasterRepository.save(req.toEntity(code));
            } catch (DataIntegrityViolationException e) {
                if (i == 1) throw e;
            }
        }
        throw BaseException.from(BaseResponseStatus.FAIL);
    }

    private ProductSku saveSkuWithGeneratedCode(String productCode, ProductDto.ProductSkuUpsertReq req, String normalizedColor, String normalizedSize) {
        String skuCode = productCode + "-" + normalizedColor + "-" + normalizedSize;
        try {
            ProductDto.ProductSkuUpsertReq normalizedReq = ProductDto.ProductSkuUpsertReq.builder()
                    .color(normalizedColor)
                    .size(normalizedSize)
                    .unitPrice(req.getUnitPrice())
                    .status(req.getStatus())
                    .build();
            return productSkuRepository.save(normalizedReq.toEntity(skuCode, productCode));
        } catch (DataIntegrityViolationException e) {
            throw BaseException.from(BaseResponseStatus.DUPLICATE_PRODUCT_SKU_OPTION);
        }
    }

    private void validateSkuDuplicate(String productCode, String color, String size, String selfSkuCode) {
        List<ProductSku> sameProductSkus = productSkuRepository.findByProductCodeOrderByIdDesc(productCode);
        boolean duplicated = sameProductSkus.stream()
                .anyMatch(s -> s.getColor().equalsIgnoreCase(color)
                        && s.getSize().equalsIgnoreCase(size)
                        && (selfSkuCode == null || !s.getSkuCode().equals(selfSkuCode)));
        if (duplicated) {
            throw BaseException.from(BaseResponseStatus.DUPLICATE_PRODUCT_SKU_OPTION);
        }
    }

    private String nextProductCode(String codePrefix) {
        long max = productMasterRepository.findAllByOrderByIdDesc().stream()
                .map(ProductMaster::getCode)
                .filter(Objects::nonNull)
                .map(PRODUCT_CODE_PATTERN::matcher)
                .filter(Matcher::matches)
                .filter(m -> ("PRD-" + m.group(1) + "-" + m.group(2)).equals(codePrefix))
                .mapToLong(m -> Long.parseLong(m.group(3)))
                .max()
                .orElse(0L);
        return String.format("%s-%03d", codePrefix, max + 1);
    }

    private String normalizeAndValidateColor(String colorRaw) {
        String color = colorRaw == null ? "" : colorRaw.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_COLORS.contains(color)) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
        return color;
    }

    private String normalizeAndValidateSize(String sizeRaw) {
        String size = sizeRaw == null ? "" : sizeRaw.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_SIZES.contains(size)) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
        return size;
    }
}
