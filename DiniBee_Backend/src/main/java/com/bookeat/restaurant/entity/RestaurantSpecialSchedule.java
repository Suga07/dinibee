package com.bookeat.restaurant.entity;
import com.bookeat.common.entity.BaseEntity;
import com.bookeat.owner.entity.RestaurantOwner;
import com.bookeat.restaurant.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;
import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="restaurant_special_schedules")
public class RestaurantSpecialSchedule extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="restaurant_id",nullable=false)
    private Restaurant restaurant;
    @Column(nullable=false)
    private LocalDate date;
    @Column(nullable=false)
    private boolean closed;
    @Column(nullable=true)
    private LocalTime openingTime;
    @Column(nullable=true)
    private LocalTime closingTime;
    @Column(nullable=false)
    private boolean closesNextDay;
    @Column(nullable=false,length=500)
    private String reason;
}
