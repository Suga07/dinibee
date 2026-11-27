package com.bookeat.membership.dto;
import jakarta.validation.constraints.*;import java.math.BigDecimal;import java.time.Instant;import java.util.*;
public final class MembershipDtos {
 private MembershipDtos() {}
 public record Plan(UUID id,String name,String description,BigDecimal price,String billingPeriod,String badge,List<String> benefits,int displayOrder,boolean active,Instant createdAt,Instant updatedAt) {}
 public record SavePlan(@NotBlank @Size(max=100) String name,@Size(max=500) String description,@NotNull @DecimalMin("0") BigDecimal price,@NotBlank @Pattern(regexp="MONTHLY|YEARLY|LIFETIME") String billingPeriod,@Size(max=80) String badge,List<@Size(max=200) String> benefits,@Min(0) int displayOrder,@NotNull Boolean active) {}
 public record Subscribe(@NotNull UUID planId) {}
 public record Subscription(UUID id,UUID planId,String planName,String status,Instant startsAt,Instant endsAt) {}
 public record Checkout(UUID id,UUID planId,String planName,BigDecimal amount,String status,String gatewayOrderId,String publicKey,String currency) {}
 public record VerifyPayment(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{6,100}") String gatewayPaymentId,@NotBlank @Pattern(regexp="[a-fA-F0-9]{64}") String signature) {}
}
