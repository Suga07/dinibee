package com.bookeat.admin.dto;
import jakarta.validation.constraints.*;import com.bookeat.common.validation.ValidPassword;import com.bookeat.user.entity.RoleCode;import com.bookeat.restaurant.enums.ServiceType;import java.util.*;import java.math.BigDecimal;import java.time.Instant;
public final class AdminRequests {
 private AdminRequests(){}
 public record Action(@NotNull @PositiveOrZero Long version,@NotBlank @Size(max=1000) String reason){}
 public record Create(@NotBlank @Size(max=80) String firstName,@NotBlank @Size(max=80) String lastName,@NotBlank @Email @Size(max=320) String email,@ValidPassword String password,@NotNull @Size(min=1,max=7) Set<@NotNull RoleCode> roles){}
 public record Roles(@NotNull @PositiveOrZero Long version,@NotNull @Size(min=1,max=7) Set<@NotNull RoleCode> roles,@NotBlank @Size(max=1000) String reason){}
 public record Setting(@NotBlank @Size(max=1000) String value,@NotBlank @Size(max=1000) String reason){}
 public record Promotion(@NotBlank @Pattern(regexp="[A-Z0-9_-]{3,40}") String code,@NotBlank @Size(max=160) String title,UUID restaurantId,ServiceType serviceType,@NotNull @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal minimumAmount,@NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer=3,fraction=2) BigDecimal discountPercent,@NotNull @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal maximumDiscount,@Min(1) int maximumUses,@NotNull Instant effectiveFrom,@NotNull Instant effectiveUntil,@NotNull Boolean active){}
 public record Coupon(@NotNull UUID restaurantId,@NotBlank @Pattern(regexp="[A-Z0-9_-]{3,40}") String code,@NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=12,fraction=2) BigDecimal amount,@NotNull ServiceType serviceType){}
}
