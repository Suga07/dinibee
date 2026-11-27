package com.bookeat.menu.dto;
import com.bookeat.menu.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
public final class MenuRequests {
    private MenuRequests() {}
    public record Category(@NotNull @PositiveOrZero Long restaurantVersion,@NotBlank @Size(max=100) String name,@Size(max=2000) String description,@Size(max=2048) @Pattern(regexp="https?://[^\\s]+") String imageUrl,@NotNull @Min(0) @Max(100000) Integer displayOrder,@NotNull Boolean active) {}
    public record Option(UUID id,@NotBlank @Size(max=80) String name,@NotNull @DecimalMin("0.00") @DecimalMax("9999999999.99") @Digits(integer=10,fraction=2) BigDecimal price,@NotNull Boolean active) {}
    public record Item(
        @NotNull @PositiveOrZero Long restaurantVersion,@NotNull UUID categoryId,
        @NotBlank @Size(max=160) String name,@Size(max=3000) String description,
        @NotNull @DecimalMin("0.00") @DecimalMax("9999999999.99") @Digits(integer=10,fraction=2) BigDecimal basePrice,
        @DecimalMin("0.00") @DecimalMax("9999999999.99") @Digits(integer=10,fraction=2) BigDecimal discountPrice,
        @NotNull FoodType foodType,@NotNull SpiceLevel spiceLevel,
        @Size(max=2048) @Pattern(regexp="https?://[^\\s]+") String imageUrl,
        @NotNull Boolean available,@NotNull Boolean preOrderAvailable,@NotNull Boolean takeawayAvailable,@NotNull Boolean diniTicketPreOrderAvailable,
        @NotNull @Min(0) @Max(1440) Integer preparationTimeMinutes,@NotNull Boolean featured,@NotNull @Min(0) @Max(100000) Integer displayOrder,
        @NotNull @Size(max=50) List<@NotNull @Valid Option> variants,@NotNull @Size(max=50) List<@NotNull @Valid Option> addons) {}
    public record Availability(@NotNull @PositiveOrZero Long restaurantVersion,@NotNull Boolean available) {}
}
