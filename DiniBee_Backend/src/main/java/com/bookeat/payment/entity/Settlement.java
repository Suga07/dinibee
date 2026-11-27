package com.bookeat.payment.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="settlements") @Getter @Setter @NoArgsConstructor
public class Settlement extends BaseEntity {
    @Column(nullable=false) private UUID restaurantId;
    @Column(nullable=false) private Instant periodStart;
    @Column(nullable=false) private Instant periodEnd;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal grossAmount;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal commissionAmount;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal gatewayFee;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal taxAmount;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal refundAmount;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal netAmount;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=20) private com.bookeat.payment.enums.SettlementStatus status;
    @Column(nullable=true,length=100) private String settlementReference;
    @Column(nullable=true) private Instant processedAt;
}
