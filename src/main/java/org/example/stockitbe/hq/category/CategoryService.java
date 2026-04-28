package org.example.stockitbe.hq.category;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.category.model.Category;
import org.example.stockitbe.hq.category.model.CategoryDto;
import org.example.stockitbe.hq.category.model.CategoryLevel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryDto.TreeRes> findAllTree() {
        List<Category> roots = categoryRepository.findAllByParentIdIsNullOrderBySortOrderAscIdAsc();
        List<CategoryDto.TreeRes> result = new ArrayList<>();

        for (Category root : roots) {
            List<Category> children = categoryRepository.findAllByParentIdOrderBySortOrderAscIdAsc(root.getId());
            List<CategoryDto.TreeRes> childRes = children.stream()
                    .map(child -> CategoryDto.TreeRes.from(child, root.getCode(), List.of()))
                    .toList();
            result.add(CategoryDto.TreeRes.from(root, null, childRes));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public CategoryDto.DetailRes findByCode(String code) {
        Category category = lookupCategory(code);
        String parentCode = null;
        if (category.getParentId() != null) {
            Category parent = categoryRepository.findById(category.getParentId())
                    .orElseThrow(() -> BaseException.from(BaseResponseStatus.CATEGORY_PARENT_NOT_FOUND));
            parentCode = parent.getCode();
        }
        return CategoryDto.DetailRes.from(category, parentCode);
    }

    @Transactional
    public CategoryDto.DetailRes create(CategoryDto.CreateReq req) {
        if (req.getLevel() == CategoryLevel.ROOT) {
            validateRootCreate(req);
            String code = generateCode();
            int sortOrder = (int) categoryRepository.countByParentIdIsNull() + 1;
            Category saved = categoryRepository.save(req.toEntity(code, null, sortOrder));
            return CategoryDto.DetailRes.from(saved, null);
        }

        Category parent = resolveParent(req.getParentCode());
        validateChildCreate(req, parent);
        String code = generateCode();
        int sortOrder = (int) categoryRepository.countByParentId(parent.getId()) + 1;
        Category saved = categoryRepository.save(req.toEntity(code, parent.getId(), sortOrder));
        return CategoryDto.DetailRes.from(saved, parent.getCode());
    }

    private void validateRootCreate(CategoryDto.CreateReq req) {
        if (req.getParentCode() != null && !req.getParentCode().isBlank()) {
            throw BaseException.from(BaseResponseStatus.CATEGORY_ROOT_PARENT_DISALLOWED);
        }
        boolean duplicated = categoryRepository.existsByParentIdIsNullAndNameIgnoreCase(req.getName().trim());
        if (duplicated) {
            throw BaseException.from(BaseResponseStatus.DUPLICATE_CATEGORY_NAME);
        }
    }

    private void validateChildCreate(CategoryDto.CreateReq req, Category parent) {
        boolean duplicated = categoryRepository.existsByParentIdAndNameIgnoreCase(parent.getId(), req.getName().trim());
        if (duplicated) {
            throw BaseException.from(BaseResponseStatus.DUPLICATE_CATEGORY_NAME);
        }
    }

    private Category resolveParent(String parentCode) {
        if (parentCode == null || parentCode.isBlank()) {
            throw BaseException.from(BaseResponseStatus.CATEGORY_PARENT_REQUIRED);
        }
        Category parent = lookupCategory(parentCode);
        if (parent.getLevel() != CategoryLevel.ROOT) {
            throw BaseException.from(BaseResponseStatus.CATEGORY_PARENT_NOT_ROOT);
        }
        return parent;
    }

    private Category lookupCategory(String code) {
        return categoryRepository.findByCode(code)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.CATEGORY_NOT_FOUND));
    }

    private String generateCode() {
        long seq = categoryRepository.count() + 1;
        return String.format("CAT-%04d", seq);
    }
}
