package com.bookeat.restaurant.entity;
import com.bookeat.common.entity.BaseEntity;
import com.bookeat.owner.entity.RestaurantOwner;
import com.bookeat.restaurant.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;
import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="restaurant_service_activations")
public class RestaurantServiceActivation extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="restaurant_id",nullable=false)
    private Restaurant restaurant;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private ServiceType serviceType;
    @Column(nullable=false)
    private boolean requested;
    @Column(nullable=false)
    private boolean authorized;
    @Column(nullable=false)
    private boolean enabled;
    @Column(nullable=true)
    private Instant activatedAt;
    @Column(nullable=true)
    private Instant disabledAt;
}
