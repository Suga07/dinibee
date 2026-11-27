package com.bookeat.restaurant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/** Owner-entered onboarding plans. Does not grant staff access or publish a menu. */
public record OnboardingForm(
    @Size(max=2048) String website,
    @Size(max=300) String tagline,
    @Size(max=100) String cuisineType,
    @NotBlank @Size(max=160) String businessName,
    @Pattern(regexp="[A-Z]{5}[0-9]{4}[A-Z]") String panNumber,
    @Size(max=250) String landmark,
    boolean waitlistEnabled,
    boolean customerTableSelection,
    boolean takeawayEnabled,
    boolean asapPickupEnabled,
    boolean scheduledPickupEnabled,
    @Min(1) @Max(240) Integer preparationTime,
    @DecimalMin("0") @Digits(integer=8,fraction=2) BigDecimal minimumOrderValue,
    @DecimalMin("0") @Digits(integer=8,fraction=2) BigDecimal packagingCharge,
    @Size(max=2000) String pickupInstructions,
    @Size(max=80) String payoutSchedule,
    @Size(max=2000) String noShowPolicy,
    @Size(max=2000) String lateArrivalPolicy,
    @Size(max=2000) String takeawayCancellationPolicy,
    @NotNull @Size(max=1000) List<@Valid Table> tables,
    @NotNull @Size(max=100) List<@Valid Category> menuCategories,
    @NotNull @Size(max=100) List<@Valid Staff> staff
) {
    public record Table(@NotBlank @Size(max=80) String tableNumber,
        @Min(1) @Max(30) int capacity, @Size(max=100) String location, boolean available) {}
    public record Category(@NotBlank @Size(max=100) String name,
        @NotNull @Size(max=500) List<@Valid Item> items) {}
    public record Item(@NotBlank @Size(max=160) String name, @Size(max=3000) String description,
        @NotNull @DecimalMin("0") @Digits(integer=8,fraction=2) BigDecimal price,
        @Size(max=100) String category, boolean vegetarian, boolean dineIn, boolean takeaway) {}
    public record Staff(@NotBlank @Size(max=160) String name, @Size(max=30) String phone,
        @NotBlank @Email @Size(max=320) String email, @NotBlank @Size(max=50) String role,
        @NotNull @Size(max=100) List<@Size(max=100) String> permissions) {}
}
