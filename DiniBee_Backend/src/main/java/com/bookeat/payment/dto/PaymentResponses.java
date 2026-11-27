package com.bookeat.payment.dto;
import com.bookeat.payment.enums.*;import java.util.*;import java.time.Instant;import java.math.BigDecimal;
public final class PaymentResponses {
 private PaymentResponses(){}
 public record View(UUID id,long version,PaymentReferenceType referenceType,UUID referenceId,UUID restaurantId,PaymentGateway gateway,String gatewayOrderId,String gatewayPaymentId,BigDecimal amount,BigDecimal discountAmount,String currency,PaymentStatus status,String paymentMethod,Instant verifiedAt,BigDecimal refundedAmount,String publicKey,boolean reconciliationRequired,Checkout checkout){}
 public record Checkout(String provider,String paymentSessionId,String accountId,String apiKey,String domain,boolean sandbox){}
 public record RefundView(UUID id,long version,UUID paymentId,BigDecimal amount,String reason,RefundStatus status,String gatewayRefundId,Instant requestedAt,Instant processedAt){}
 public record CommissionView(UUID id,long version,String serviceType,CommissionType commissionType,BigDecimal commissionValue,Instant effectiveFrom,Instant effectiveUntil,boolean active){}
 public record SettlementView(UUID id,long version,UUID restaurantId,Instant periodStart,Instant periodEnd,BigDecimal grossAmount,BigDecimal commissionAmount,BigDecimal gatewayFee,BigDecimal taxAmount,BigDecimal refundAmount,BigDecimal netAmount,SettlementStatus status,String settlementReference,Instant processedAt){}
}
