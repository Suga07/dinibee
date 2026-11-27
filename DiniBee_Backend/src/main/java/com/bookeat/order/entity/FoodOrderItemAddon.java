package com.bookeat.order.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="food_order_item_addons") @Getter @Setter @NoArgsConstructor
public class FoodOrderItemAddon extends BaseEntity {
    @Column(nullable=false) private UUID orderItemId;
    @Column(nullable=false) private UUID menuAddonId;
    @Column(nullable=false,length=80) private String name;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal price;
}
