package com.bookeat.order.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="takeaway_orders") @Getter @Setter @NoArgsConstructor
public class TakeawayOrder extends BaseEntity {
    @Column(nullable=false,length=40) private String orderNumber;
    @Column(nullable=false) private UUID foodOrderId;
    @Column(nullable=false) private UUID customerId;
    @Column(nullable=false) private UUID restaurantId;
    @Column(nullable=false) private Instant pickupTime;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30) private com.bookeat.order.enums.TakeawayStatus status;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal subtotal;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal discount;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal tax;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal total;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30) private com.bookeat.order.enums.OrderPaymentStatus paymentStatus;
    @Column(nullable=false,length=64) private String pickupCode;
    @Column(nullable=true) private Instant collectedAt;
}
