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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final Map<String, String> ROOT_NAME_TO_CODE = Map.of(
            "상의", "CAT-L1-TOP",
            "바지", "CAT-L1-PNT",
            "치마", "CAT-L1-SKT",
            "아우터", "CAT-L1-OUT"
    );

    private static final Map<String, String> CHILD_NAME_TO_CODE_BY_PARENT = buildChildCodeMap();

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
            Category saved = categoryRepository.save(req.toEntity(resolveRootCode(req.getName()), null, sortOrder));
            return CategoryDto.DetailRes.from(saved, null);
        }

        Category parent = resolveParent(req.getParentCode());
        validateChildCreate(req, parent);
        int sortOrder = (int) categoryRepository.countByParentId(parent.getId()) + 1;
        Category saved = categoryRepository.save(req.toEntity(resolveChildCode(parent.getCode(), req.getName()), parent.getId(), sortOrder));
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
            // 고정 코드셋 정책: 루트 이름 변경 시에도 허용된 이름만 가능
            resolveRootCode(normalizedName);
        } else {
            boolean duplicated = categoryRepository
                    .existsByParentIdAndNameIgnoreCaseAndIdNot(category.getParentId(), normalizedName, category.getId());
            if (duplicated) {
                throw BaseException.from(BaseResponseStatus.DUPLICATE_CATEGORY_NAME);
            }
            String parentCode = resolveParentCode(category);
            resolveChildCode(parentCode, normalizedName);
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
        resolveRootCode(req.getName());
        boolean duplicated = categoryRepository.existsByParentIdIsNullAndNameIgnoreCase(req.getName().trim());
        if (duplicated) {
            throw BaseException.from(BaseResponseStatus.DUPLICATE_CATEGORY_NAME);
        }
    }

    private void validateChildCreate(CategoryDto.CreateReq req, Category parent) {
        resolveChildCode(parent.getCode(), req.getName());
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

    private String resolveRootCode(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        String code = ROOT_NAME_TO_CODE.get(name);
        if (code == null) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
        return code;
    }

    private String resolveChildCode(String parentCode, String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        String key = parentCode + "|" + name;
        String code = CHILD_NAME_TO_CODE_BY_PARENT.get(key);
        if (code == null) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
        return code;
    }

    private static Map<String, String> buildChildCodeMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("CAT-L1-TOP|반팔", "CAT-L2-TOP-SS");
        map.put("CAT-L1-TOP|긴팔", "CAT-L2-TOP-LS");
        map.put("CAT-L1-TOP|셔츠", "CAT-L2-TOP-SH");
        map.put("CAT-L1-TOP|니트", "CAT-L2-TOP-KN");
        map.put("CAT-L1-TOP|후드티", "CAT-L2-TOP-HD");

        map.put("CAT-L1-PNT|청바지", "CAT-L2-PNT-DN");
        map.put("CAT-L1-PNT|반바지", "CAT-L2-PNT-ST");
        map.put("CAT-L1-PNT|긴바지", "CAT-L2-PNT-LG");
        map.put("CAT-L1-PNT|츄리닝", "CAT-L2-PNT-TR");

        map.put("CAT-L1-SKT|미니스커트", "CAT-L2-SKT-MN");
        map.put("CAT-L1-SKT|롱스커트", "CAT-L2-SKT-LG");

        map.put("CAT-L1-OUT|패딩", "CAT-L2-OUT-PD");
        map.put("CAT-L1-OUT|후드집업", "CAT-L2-OUT-HZ");
        map.put("CAT-L1-OUT|자켓", "CAT-L2-OUT-JK");
        map.put("CAT-L1-OUT|가디건", "CAT-L2-OUT-CD");
        return Map.copyOf(map);
    }
}
