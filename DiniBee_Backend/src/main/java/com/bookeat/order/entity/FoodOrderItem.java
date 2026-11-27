package com.bookeat.order.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="food_order_items") @Getter @Setter @NoArgsConstructor
public class FoodOrderItem extends BaseEntity {
    @Column(nullable=false) private UUID orderId;
    @Column(nullable=false) private UUID menuItemId;
    @Column(nullable=true) private UUID variantId;
    @Column(nullable=false,length=160) private String itemName;
    @Column(nullable=true,length=2000) private String imageUrl;
    @Column(nullable=true,length=80) private String variantName;
    @Column(nullable=false) private int quantity;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal baseUnitPrice;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal unitPrice;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal subtotal;
    @Column(nullable=true,length=1000) private String notes;
}
