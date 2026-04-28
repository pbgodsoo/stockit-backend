package org.example.stockitbe.hq.vendor;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.vendor.model.VendorProductDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendor-products")
@RequiredArgsConstructor
public class VendorProductController {

    private final VendorProductService service;

    @GetMapping
    public BaseResponse<List<VendorProductDto.ListRes>> list(@RequestParam String vendorCode) {
        return BaseResponse.success(service.findByVendor(vendorCode));
    }

    @GetMapping("/{code}")
    public BaseResponse<VendorProductDto.DetailRes> detail(@PathVariable String code) {
        return BaseResponse.success(service.findByCode(code));
    }

    @PostMapping
    public BaseResponse<VendorProductDto.DetailRes> create(@Valid @RequestBody VendorProductDto.CreateReq req) {
        return BaseResponse.success(service.create(req));
    }

    @PatchMapping("/{code}")
    public BaseResponse<VendorProductDto.DetailRes> update(@PathVariable String code,
                                                            @Valid @RequestBody VendorProductDto.UpdateReq req) {
        return BaseResponse.success(service.update(code, req));
    }

    @PatchMapping("/{code}/status")
    public BaseResponse<VendorProductDto.DetailRes> updateStatus(@PathVariable String code,
                                                                  @Valid @RequestBody VendorProductDto.StatusUpdateReq req) {
        return BaseResponse.success(service.updateStatus(code, req));
    }

    @DeleteMapping("/{code}")
    public BaseResponse<Void> delete(@PathVariable String code) {
        service.delete(code);
        return BaseResponse.success(null);
    }
}
