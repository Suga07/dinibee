package com.bookeat.menu.dto;
import com.bookeat.menu.enums.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
public final class MenuResponses {
    private MenuResponses() {}
    public record Category(UUID id,long version,UUID restaurantId,String name,String description,String imageUrl,int displayOrder,boolean active,Instant createdAt,Instant updatedAt) {}
    public record Option(UUID id,long version,String name,BigDecimal price,boolean active) {}
    public record Item(UUID id,long version,UUID restaurantId,UUID categoryId,String categoryName,String name,String description,BigDecimal basePrice,BigDecimal discountPrice,BigDecimal sellingPrice,FoodType foodType,SpiceLevel spiceLevel,String imageUrl,boolean available,boolean effectiveAvailable,boolean preOrderAvailable,boolean takeawayAvailable,boolean diniTicketPreOrderAvailable,int preparationTimeMinutes,boolean featured,int displayOrder,List<Option> variants,List<Option> addons,Instant createdAt,Instant updatedAt) {}
}
