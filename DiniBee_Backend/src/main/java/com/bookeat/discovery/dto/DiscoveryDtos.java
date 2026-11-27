package com.bookeat.discovery.dto;
import jakarta.validation.constraints.*;
import com.bookeat.restaurant.enums.ServiceType;
import com.bookeat.menu.enums.FoodType;
import java.util.*;
import java.math.BigDecimal;
public final class DiscoveryDtos {
    private DiscoveryDtos() {}
    public record Search(@Size(max=160) String restaurantName,@Size(max=100) String city,@DecimalMin("-90") @DecimalMax("90") Double latitude,@DecimalMin("-180") @DecimalMax("180") Double longitude,@DecimalMin(value="0",inclusive=false) @DecimalMax("500") Double radius,@Size(max=80) String cuisine,@DecimalMin("0") @DecimalMax("5") Double rating,FoodType foodType,ServiceType serviceType,Boolean openNow,Boolean offerAvailable,@Pattern(regexp="NAME|RATING|DISTANCE") String sort,@Min(0) @Max(10000) Integer page,@Min(1) @Max(100) Integer size) {}
    public record Card(UUID id,String restaurantName,String description,String city,String addressLine1,String logoUrl,String coverImageUrl,BigDecimal latitude,BigDecimal longitude,String timezone,Double distanceKm,BigDecimal averageRating,long reviewCount,boolean openNow,boolean offerAvailable,List<String> cuisines,List<String> enabledServices) {}
    public record PublicDetail(Card restaurant,String phone,String addressLine2,String state,String postalCode,String country,String restaurantType,String openingTime,String closingTime,boolean closedToday) {}
    public record Cuisines(@NotNull @PositiveOrZero Long restaurantVersion,@NotNull @Size(max=20) List<@NotBlank @Size(max=80) String> cuisines) {}
}
