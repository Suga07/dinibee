package com.bookeat.order.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="restaurant_order_settings") @Getter @Setter @NoArgsConstructor
public class RestaurantOrderSettings extends BaseEntity {
    @Column(nullable=false) private UUID restaurantId;
    @Column(nullable=false,precision=5,scale=2) private BigDecimal taxPercent;
}
