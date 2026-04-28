package org.example.stockitbe.hq.category;

import org.example.stockitbe.hq.category.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByCode(String code);

    List<Category> findAllByOrderByIdAsc();

    List<Category> findAllByParentIdIsNullOrderBySortOrderAscIdAsc();

    List<Category> findAllByParentIdOrderBySortOrderAscIdAsc(Long parentId);

    boolean existsByParentIdIsNullAndNameIgnoreCase(String name);

    boolean existsByParentIdAndNameIgnoreCase(Long parentId, String name);

    boolean existsByParentIdIsNullAndNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByParentIdAndNameIgnoreCaseAndIdNot(Long parentId, String name, Long id);

    boolean existsByParentId(Long parentId);

    long countByParentIdIsNull();

    long countByParentId(Long parentId);

    long count();
}
