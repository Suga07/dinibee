package com.bookeat.order.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="order_status_history") @Getter @Setter @NoArgsConstructor
public class OrderStatusHistory extends BaseEntity {
    @Column(nullable=false) private UUID orderId;
    @Enumerated(EnumType.STRING)
    @Column(nullable=true,length=30) private com.bookeat.order.enums.FoodOrderStatus oldStatus;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30) private com.bookeat.order.enums.FoodOrderStatus newStatus;
    @Column(nullable=true) private UUID actorId;
    @Column(nullable=true,length=1000) private String reason;
    @Column(nullable=false) private Instant changedAt;
}
