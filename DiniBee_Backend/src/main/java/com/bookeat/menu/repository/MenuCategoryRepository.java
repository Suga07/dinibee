package com.bookeat.menu.repository;
import com.bookeat.menu.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.*;
public interface MenuCategoryRepository extends JpaRepository<MenuCategory,UUID> {
    Optional<MenuCategory> findByIdAndRestaurantIdAndDeletedAtIsNull(UUID id,UUID restaurantId);
    Page<MenuCategory> findByRestaurantIdAndDeletedAtIsNull(UUID restaurantId,Pageable page);
    boolean existsByRestaurantIdAndNameIgnoreCaseAndDeletedAtIsNullAndIdNot(UUID restaurantId,String name,UUID id);
    long countByRestaurantIdAndDeletedAtIsNull(UUID restaurantId);
}
