package com.bookeat.menu.repository;
import com.bookeat.menu.entity.MenuAddon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface MenuAddonRepository extends JpaRepository<MenuAddon,UUID> {
    List<MenuAddon> findByMenuItemIdAndDeletedAtIsNullOrderByNameAscIdAsc(UUID menuItemId);
    List<MenuAddon> findByMenuItemIdInAndDeletedAtIsNullOrderByNameAscIdAsc(Collection<UUID> ids);
}
