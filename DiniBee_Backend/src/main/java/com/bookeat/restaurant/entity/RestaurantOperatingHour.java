package com.bookeat.restaurant.entity;
import com.bookeat.common.entity.BaseEntity;
import com.bookeat.owner.entity.RestaurantOwner;
import com.bookeat.restaurant.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;
import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="restaurant_operating_hours")
public class RestaurantOperatingHour extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="restaurant_id",nullable=false)
    private Restaurant restaurant;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private DayOfWeek dayOfWeek;
    @Column(nullable=true)
    private LocalTime openingTime;
    @Column(nullable=true)
    private LocalTime closingTime;
    @Column(nullable=false)
    private boolean closed;
    @Column(nullable=false)
    private boolean closesNextDay;
}
