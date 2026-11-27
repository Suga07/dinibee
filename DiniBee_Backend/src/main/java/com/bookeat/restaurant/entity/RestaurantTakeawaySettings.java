package com.bookeat.restaurant.entity;
import com.bookeat.common.entity.BaseEntity;
import com.bookeat.owner.entity.RestaurantOwner;
import com.bookeat.restaurant.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;
import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="restaurant_takeaway_settings")
public class RestaurantTakeawaySettings extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="restaurant_id",nullable=false,unique=true)
    private Restaurant restaurant;
    @Column(nullable=false)
    private boolean enabled;
    @Column(nullable=false)
    private boolean regularTakeawayEnabled;
    @Column(nullable=false)
    private boolean onTheWayEnabled;
    @Column(nullable=false,precision=12,scale=2)
    private BigDecimal minimumOrderAmount;
    @Column(nullable=false)
    private int averagePreparationMinutes;
    @Column(nullable=false)
    private int maximumAdvanceOrderMinutes;
    @Column(nullable=false)
    private boolean acceptCash;
    @Column(nullable=false)
    private boolean acceptOnlinePayment;
}
