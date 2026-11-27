package com.bookeat.menu.repository;
import com.bookeat.menu.entity.MenuItem;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface MenuItemRepository extends JpaRepository<MenuItem,UUID>,JpaSpecificationExecutor<MenuItem> {
    @Override @EntityGraph(attributePaths="category")
    org.springframework.data.domain.Page<MenuItem> findAll(org.springframework.data.jpa.domain.Specification<MenuItem> spec,org.springframework.data.domain.Pageable pageable);
    Optional<MenuItem> findByIdAndRestaurantIdAndDeletedAtIsNull(UUID id,UUID restaurantId);
    boolean existsByRestaurantIdAndCategoryIdAndDeletedAtIsNull(UUID restaurantId,UUID categoryId);
    boolean existsByRestaurantIdAndCategoryIdAndNameIgnoreCaseAndDeletedAtIsNullAndIdNot(UUID restaurantId,UUID categoryId,String name,UUID id);
    long countByRestaurantIdAndDeletedAtIsNull(UUID restaurantId);
}
