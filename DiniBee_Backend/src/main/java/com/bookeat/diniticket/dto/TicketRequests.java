package com.bookeat.diniticket.dto;
import jakarta.validation.constraints.*;
import java.util.UUID;
public final class TicketRequests {
    private TicketRequests() {}
    public record Create(@NotNull UUID restaurantId,@Min(1) @Max(500) int guestCount) {}
    public record Action(@NotNull @PositiveOrZero Long version,@NotBlank @Size(max=1000) String reason) {}
    public record Seat(@NotNull @PositiveOrZero Long version,UUID tableId,@NotBlank @Size(max=1000) String reason) {}
    public record Handover(@NotNull @PositiveOrZero Long version,@NotBlank @Size(max=50) String tokenNumber) {}
}
