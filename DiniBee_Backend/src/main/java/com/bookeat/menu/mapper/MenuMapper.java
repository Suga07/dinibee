package com.bookeat.menu.mapper;
import com.bookeat.menu.entity.*;
import com.bookeat.menu.dto.MenuResponses.*;
import com.bookeat.menu.service.MenuPricing;
import com.bookeat.restaurant.enums.ServiceType;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
public class MenuMapper {
    public Category category(MenuCategory c){return new Category(c.getId(),c.getVersion(),c.getRestaurant().getId(),c.getName(),c.getDescription(),c.getImageUrl(),c.getDisplayOrder(),c.isActive(),c.getCreatedAt(),c.getUpdatedAt());}
    public Option variant(MenuVariant v){return new Option(v.getId(),v.getVersion(),v.getName(),v.getPrice(),v.isActive());}
    public Option addon(MenuAddon v){return new Option(v.getId(),v.getVersion(),v.getName(),v.getPrice(),v.isActive());}
    public Item item(MenuItem e,List<MenuVariant> variants,List<MenuAddon> addons,Set<ServiceType> services) {
        boolean effective=e.isAvailable() && e.getDeletedAt()==null && e.getCategory().isActive() && e.getCategory().getDeletedAt()==null &&
            (services.contains(ServiceType.DINE_IN) || (services.contains(ServiceType.TAKEAWAY) && e.isTakeawayAvailable()) || (services.contains(ServiceType.DINITICKET) && e.isDiniTicketPreOrderAvailable()));
        return new Item(e.getId(),e.getVersion(),e.getRestaurant().getId(),e.getCategory().getId(),e.getCategory().getName(),e.getName(),e.getDescription(),e.getBasePrice(),e.getDiscountPrice(),MenuPricing.sellingPrice(e.getBasePrice(),e.getDiscountPrice()),e.getFoodType(),e.getSpiceLevel(),e.getImageUrl(),e.isAvailable(),effective,e.isPreOrderAvailable(),e.isTakeawayAvailable(),e.isDiniTicketPreOrderAvailable(),e.getPreparationTimeMinutes(),e.isFeatured(),e.getDisplayOrder(),variants.stream().map(this::variant).toList(),addons.stream().map(this::addon).toList(),e.getCreatedAt(),e.getUpdatedAt());
    }
}
