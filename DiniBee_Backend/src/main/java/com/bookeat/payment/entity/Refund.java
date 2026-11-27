package com.bookeat.payment.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="refunds") @Getter @Setter @NoArgsConstructor
public class Refund extends BaseEntity {
    @Column(nullable=false) private UUID paymentId;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal amount;
    @Column(nullable=false,length=1000) private String reason;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=20) private com.bookeat.payment.enums.RefundStatus status;
    @Column(nullable=true,length=100) private String gatewayRefundId;
    @Column(nullable=false) private UUID requestedBy;
    @Column(nullable=true) private UUID approvedBy;
    @Column(nullable=false) private Instant requestedAt;
    @Column(nullable=true) private Instant processedAt;
    @Column(nullable=true) private Instant dispatchStartedAt;
    @Column(nullable=true,length=200) private String failureReason;
}
