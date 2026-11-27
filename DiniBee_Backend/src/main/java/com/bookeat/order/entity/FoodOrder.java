package com.bookeat.order.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="food_orders") @Getter @Setter @NoArgsConstructor
public class FoodOrder extends BaseEntity {
    @Column(nullable=false,precision=5,scale=2) private BigDecimal taxPercent;
    @Column(nullable=false,length=40) private String orderNumber;
    @Column(nullable=false) private UUID customerId;
    @Column(nullable=false) private UUID restaurantId;
    @Column(nullable=true) private UUID reservationId;
    @Column(nullable=true) private UUID groupBookingId;
    @Column(nullable=true) private UUID corporateBookingId;
    @Column(nullable=true) private UUID takeawayOrderId;
    @Column(nullable=true) private UUID diniTicketId;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=40) private com.bookeat.order.enums.OrderType orderType;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30) private com.bookeat.order.enums.FoodOrderStatus status;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal subtotal;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal discount;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal tax;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal total;
    @Column(nullable=false,length=3) private String currency;
    @Column(nullable=true,length=2000) private String specialInstructions;
}
