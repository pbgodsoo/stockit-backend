package org.example.stockitbe.hq.product;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.product.model.ProductDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hq")
@RequiredArgsConstructor
public class ProductMasterController {

    private final ProductMasterService service;

    @GetMapping("/products")
    public BaseResponse<List<ProductDto.ProductMasterRes>> listProducts(@RequestParam(required = false) String keyword,
                                                                        @RequestParam(required = false) String categoryCode) {
        return BaseResponse.success(service.findProducts(keyword, categoryCode));
    }

    @GetMapping("/products/{code}")
    public BaseResponse<ProductDto.ProductMasterRes> detailProduct(@PathVariable String code) {
        return BaseResponse.success(service.findProductByCode(code));
    }

    @PostMapping("/products")
    public BaseResponse<ProductDto.ProductMasterRes> createProduct(@Valid @RequestBody ProductDto.ProductMasterUpsertReq req) {
        return BaseResponse.success(service.createProduct(req));
    }

    @PatchMapping("/products/{code}")
    public BaseResponse<ProductDto.ProductMasterRes> updateProduct(@PathVariable String code,
                                                                    @Valid @RequestBody ProductDto.ProductMasterUpsertReq req) {
        return BaseResponse.success(service.updateProduct(code, req));
    }

    @DeleteMapping("/products/{code}")
    public BaseResponse<Void> deleteProduct(@PathVariable String code) {
        service.deleteProduct(code);
        return BaseResponse.success(null);
    }

    @GetMapping("/products/{code}/skus")
    public BaseResponse<List<ProductDto.ProductSkuRes>> listSkus(@PathVariable String code) {
        return BaseResponse.success(service.findSkus(code));
    }

    @PostMapping("/products/{code}/skus")
    public BaseResponse<ProductDto.ProductSkuRes> createSku(@PathVariable String code,
                                                            @Valid @RequestBody ProductDto.ProductSkuUpsertReq req) {
        return BaseResponse.success(service.createSku(code, req));
    }

    @PostMapping("/products/{code}/skus/bulk")
    public BaseResponse<ProductDto.ProductSkuBulkCreateRes> createSkusBulk(@PathVariable String code,
                                                                            @Valid @RequestBody ProductDto.ProductSkuBulkCreateReq req) {
        return BaseResponse.success(service.bulkCreateSkus(code, req));
    }

    @PatchMapping("/skus/{skuCode}")
    public BaseResponse<ProductDto.ProductSkuRes> updateSku(@PathVariable String skuCode,
                                                             @Valid @RequestBody ProductDto.ProductSkuUpsertReq req) {
        return BaseResponse.success(service.updateSku(skuCode, req));
    }

    @PatchMapping("/products/{code}/skus/price")
    public BaseResponse<ProductDto.ProductSkuPriceBulkUpdateRes> updateSkuPrices(@PathVariable String code,
                                                                                  @Valid @RequestBody ProductDto.ProductSkuPriceBulkUpdateReq req) {
        return BaseResponse.success(service.updateAllSkuPrices(code, req));
    }

    @PatchMapping("/products/{code}/skus/status")
    public BaseResponse<ProductDto.ProductSkuStatusBulkUpdateRes> updateSkuStatus(@PathVariable String code,
                                                                                   @Valid @RequestBody ProductDto.ProductSkuStatusBulkUpdateReq req) {
        return BaseResponse.success(service.updateAllSkuStatus(code, req));
    }

    @DeleteMapping("/skus/{skuCode}")
    public BaseResponse<Void> deleteSku(@PathVariable String skuCode) {
        service.deleteSku(skuCode);
        return BaseResponse.success(null);
    }
}
