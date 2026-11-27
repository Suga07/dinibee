package com.bookeat.payment.dto;
import jakarta.validation.constraints.*;import com.bookeat.payment.enums.*;import java.util.UUID;import java.math.BigDecimal;import java.time.Instant;
public final class PaymentRequests {
 private PaymentRequests(){}
 public record Create(@NotNull PaymentReferenceType referenceType,@NotNull UUID referenceId,@NotNull @io.swagger.v3.oas.annotations.media.Schema(allowableValues={"CASH","WALLET","RAZORPAY"}) PaymentGateway gateway,@Pattern(regexp="[A-Z0-9_-]{3,40}") String couponCode){}
 public record Verify(@NotNull UUID paymentId,@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{6,100}") String gatewayPaymentId,@NotBlank @Pattern(regexp="[a-fA-F0-9]{64}") String signature){}
 public record Action(@NotNull @PositiveOrZero Long version,@NotBlank @Size(max=1000) String reason){}
 public record RefundRequest(@NotNull UUID paymentId,@NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=16,fraction=2) BigDecimal amount,@NotBlank @Size(max=1000) String reason){}
 public record Commission(@NotNull com.bookeat.restaurant.enums.ServiceType serviceType,@NotNull CommissionType commissionType,@NotNull @DecimalMin("0") @Digits(integer=16,fraction=2) BigDecimal commissionValue,@NotNull Instant effectiveFrom,Instant effectiveUntil,@NotNull Boolean active){}
 public record SettlementCreate(@NotNull UUID restaurantId,@NotNull Instant periodStart,@NotNull Instant periodEnd){}
 public record SettlementAction(@NotNull @PositiveOrZero Long version,@NotNull SettlementStatus status,@Size(max=100) String settlementReference,@NotBlank @Size(max=1000) String reason){}
}
