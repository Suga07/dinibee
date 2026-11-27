package com.bookeat.menu.repository;
import com.bookeat.menu.entity.MenuVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface MenuVariantRepository extends JpaRepository<MenuVariant,UUID> {
    List<MenuVariant> findByMenuItemIdAndDeletedAtIsNullOrderByNameAscIdAsc(UUID menuItemId);
    List<MenuVariant> findByMenuItemIdInAndDeletedAtIsNullOrderByNameAscIdAsc(Collection<UUID> ids);
}
