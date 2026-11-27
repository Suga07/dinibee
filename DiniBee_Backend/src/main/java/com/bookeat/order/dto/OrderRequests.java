package com.bookeat.order.dto;
import com.bookeat.order.enums.OrderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
public final class OrderRequests {
    private OrderRequests() {}
    public record Line(@NotNull UUID menuItemId,UUID variantId,@NotNull @Size(max=50) List<@NotNull UUID> addonIds,@Min(1) @Max(99) int quantity,@Size(max=1000) String notes) {}
    public record Create(@NotNull UUID restaurantId,@NotNull OrderType orderType,UUID reservationId,UUID diniTicketId,@NotNull @Size(min=1,max=50) List<@NotNull @Valid Line> items,@Size(max=2000) String specialInstructions) {}
    public record Action(@NotNull @PositiveOrZero Long version,@NotBlank @Size(max=1000) String reason) {}
    public record Takeaway(@NotNull UUID restaurantId,@NotNull OffsetDateTime pickupTime,@NotNull Boolean onTheWay,@NotNull @Size(min=1,max=50) List<@NotNull @Valid Line> items,@Size(max=2000) String specialInstructions) {}
    public record Collect(@NotNull @PositiveOrZero Long version,@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{12}") String pickupCode) {}
    public record CollectByOrderId(@NotNull @PositiveOrZero Long version,@NotBlank @Pattern(regexp="[A-Za-z0-9]{4}") String orderNumber) {}
    public record Location(@NotNull Boolean consent,@NotNull @DecimalMin("-90") @DecimalMax("90") @Digits(integer=3,fraction=7) BigDecimal latitude,@NotNull @DecimalMin("-180") @DecimalMax("180") @Digits(integer=3,fraction=7) BigDecimal longitude) {}
}
