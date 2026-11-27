package com.bookeat.payment.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="payments") @Getter @Setter @NoArgsConstructor
public class Payment extends BaseEntity {
    @Column(length=32) private String gatewayAccountId;
    private Boolean gatewaySandbox;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal discountAmount;
    @Column(nullable=false) private UUID customerId;
    @Column(nullable=false) private UUID restaurantId;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30) private com.bookeat.payment.enums.PaymentReferenceType referenceType;
    @Column(nullable=false) private UUID referenceId;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=20) private com.bookeat.payment.enums.PaymentGateway gateway;
    @Column(nullable=true,length=100) private String gatewayOrderId;
    @Column(nullable=true,length=100) private String gatewayPaymentId;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal amount;
    @Column(nullable=false,length=3) private String currency;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30) private com.bookeat.payment.enums.PaymentStatus status;
    @Column(nullable=true,length=30) private String paymentMethod;
    @Column(nullable=true) private Instant verifiedAt;
    @Column(nullable=true) private Instant dispatchStartedAt;
    @Column(nullable=true,length=200) private String failureReason;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal refundedAmount;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal commissionAmount;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal gatewayFee;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal gatewayTax;
    @Column(nullable=true) private UUID commissionRuleId;
    @Column(nullable=true) private UUID settlementId;
}
