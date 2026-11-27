package com.bookeat.discovery.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="customer_favorite_restaurants") @Getter @Setter @NoArgsConstructor
public class CustomerFavoriteRestaurant extends BaseEntity {
    @Column(nullable=false) private UUID customerId;
    @Column(nullable=false) private UUID restaurantId;
}
