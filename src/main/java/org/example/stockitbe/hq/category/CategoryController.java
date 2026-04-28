package org.example.stockitbe.hq.category;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.hq.category.model.CategoryDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hq/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService service;

    @GetMapping
    public BaseResponse<List<CategoryDto.TreeRes>> list() {
        return BaseResponse.success(service.findAllTree());
    }

    @GetMapping("/{code}")
    public BaseResponse<CategoryDto.DetailRes> detail(@PathVariable String code) {
        return BaseResponse.success(service.findByCode(code));
    }

    @PostMapping
    public BaseResponse<CategoryDto.DetailRes> create(@Valid @RequestBody CategoryDto.CreateReq req) {
        return BaseResponse.success(service.create(req));
    }
}
