package com.bookeat.dining.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public final class DiningRequests {
 private DiningRequests(){}
 public record Gps(@NotNull @PositiveOrZero Long version,@NotNull Boolean consent,@NotNull @DecimalMin("-90") @DecimalMax("90") @Digits(integer=3,fraction=7) BigDecimal latitude,@NotNull @DecimalMin("-180") @DecimalMax("180") @Digits(integer=3,fraction=7) BigDecimal longitude){}
 public record Qr(@NotNull @PositiveOrZero Long version,@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{43}") String token){}
 public record Action(@NotNull @PositiveOrZero Long version,@NotBlank @Size(max=1000) String reason){}
}
