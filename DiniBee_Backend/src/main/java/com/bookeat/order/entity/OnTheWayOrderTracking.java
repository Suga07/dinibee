package com.bookeat.order.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="on_the_way_order_tracking") @Getter @Setter @NoArgsConstructor
public class OnTheWayOrderTracking extends BaseEntity {
    @Column(nullable=false) private UUID takeawayOrderId;
    @Column(nullable=false) private boolean consent;
    @Column(nullable=true) private Instant consentedAt;
    @Column(nullable=true,precision=10,scale=7) private BigDecimal customerLatitude;
    @Column(nullable=true,precision=10,scale=7) private BigDecimal customerLongitude;
    @Column(nullable=true,precision=10,scale=7) private BigDecimal restaurantLatitude;
    @Column(nullable=true,precision=10,scale=7) private BigDecimal restaurantLongitude;
    @Column(nullable=true) private Instant estimatedArrival;
    @Column(nullable=true,precision=10,scale=3) private BigDecimal distanceKm;
    @Column(nullable=true) private Instant lastLocationUpdatedAt;
}
