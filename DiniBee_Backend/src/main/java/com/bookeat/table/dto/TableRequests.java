package com.bookeat.table.dto;
import com.bookeat.table.enums.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public final class TableRequests {
    private TableRequests() {}
    public record Write(
        @NotNull @PositiveOrZero Long restaurantVersion,
        @NotBlank @Size(max=30) @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9 _-]*") String tableNumber,
        @Size(max=100) String tableName,
        @NotNull @Min(1) @Max(500) Integer capacity,
        @NotNull @Min(1) @Max(500) Integer minimumGuests,
        @NotNull @Min(1) @Max(500) Integer maximumGuests,
        @NotNull @Min(-10) @Max(200) Integer floor,
        @Size(max=100) String section,
        @NotNull TableShape shape,
        @NotNull @DecimalMin("0") @DecimalMax("100000") @Digits(integer=6,fraction=2) BigDecimal xPosition,
        @NotNull @DecimalMin("0") @DecimalMax("100000") @Digits(integer=6,fraction=2) BigDecimal yPosition,
        @NotNull @DecimalMin(value="0",inclusive=false) @DecimalMax("10000") @Digits(integer=5,fraction=2) BigDecimal width,
        @NotNull @DecimalMin(value="0",inclusive=false) @DecimalMax("10000") @Digits(integer=5,fraction=2) BigDecimal height,
        @NotNull @DecimalMin("0") @DecimalMax(value="360",inclusive=false) @Digits(integer=3,fraction=2) BigDecimal rotation,
        @NotNull Boolean active) {}
    public record Status(@NotNull @PositiveOrZero Long restaurantVersion,@NotNull TableStatus status,@NotBlank @Size(max=500) String reason) {}
}
