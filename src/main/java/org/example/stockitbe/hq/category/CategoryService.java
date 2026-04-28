package org.example.stockitbe.hq.category;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.exception.BaseException;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.example.stockitbe.hq.category.model.Category;
import org.example.stockitbe.hq.category.model.CategoryDto;
import org.example.stockitbe.hq.category.model.CategoryLevel;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private static final Pattern CATEGORY_CODE_PATTERN = Pattern.compile("^CAT-(\\d{4,})$");

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
            int sortOrder = (int) categoryRepository.countByParentIdIsNull() + 1;
            Category saved = saveWithGeneratedCode(req, null, sortOrder);
            return CategoryDto.DetailRes.from(saved, null);
        }

        Category parent = resolveParent(req.getParentCode());
        validateChildCreate(req, parent);
        int sortOrder = (int) categoryRepository.countByParentId(parent.getId()) + 1;
        Category saved = saveWithGeneratedCode(req, parent.getId(), sortOrder);
        return CategoryDto.DetailRes.from(saved, parent.getCode());
    }

    @Transactional
    public CategoryDto.DetailRes update(String code, CategoryDto.UpdateReq req) {
        Category category = lookupCategory(code);
        String normalizedName = req.getName().trim();

        if (category.getLevel() == CategoryLevel.ROOT) {
            boolean duplicated = categoryRepository
                    .existsByParentIdIsNullAndNameIgnoreCaseAndIdNot(normalizedName, category.getId());
            if (duplicated) {
                throw BaseException.from(BaseResponseStatus.DUPLICATE_CATEGORY_NAME);
            }
        } else {
            boolean duplicated = categoryRepository
                    .existsByParentIdAndNameIgnoreCaseAndIdNot(category.getParentId(), normalizedName, category.getId());
            if (duplicated) {
                throw BaseException.from(BaseResponseStatus.DUPLICATE_CATEGORY_NAME);
            }
        }

        category.updateInfo(normalizedName, req.getStatus());
        String parentCode = resolveParentCode(category);
        return CategoryDto.DetailRes.from(category, parentCode);
    }

    @Transactional
    public void delete(String code) {
        Category category = lookupCategory(code);
        if (category.getLevel() == CategoryLevel.ROOT
                && categoryRepository.existsByParentId(category.getId())) {
            throw BaseException.from(BaseResponseStatus.CATEGORY_DELETE_HAS_CHILDREN);
        }
        categoryRepository.delete(category);
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

    private String resolveParentCode(Category category) {
        if (category.getParentId() == null) {
            return null;
        }
        Category parent = categoryRepository.findById(category.getParentId())
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.CATEGORY_PARENT_NOT_FOUND));
        return parent.getCode();
    }

    private String generateCode() {
        long maxSeq = categoryRepository.findAllByOrderByIdAsc().stream()
                .map(Category::getCode)
                .mapToLong(this::extractSeq)
                .max()
                .orElse(0L);
        return String.format("CAT-%04d", maxSeq + 1);
    }

    private long extractSeq(String code) {
        if (code == null) return 0L;
        Matcher matcher = CATEGORY_CODE_PATTERN.matcher(code.trim());
        if (!matcher.matches()) return 0L;
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private Category saveWithGeneratedCode(CategoryDto.CreateReq req, Long parentId, int sortOrder) {
        for (int attempt = 0; attempt < 2; attempt++) {
            String code = generateCode();
            try {
                return categoryRepository.save(req.toEntity(code, parentId, sortOrder));
            } catch (DataIntegrityViolationException e) {
                if (attempt == 1) {
                    throw e;
                }
            }
        }
        throw BaseException.from(BaseResponseStatus.FAIL);
    }
}
