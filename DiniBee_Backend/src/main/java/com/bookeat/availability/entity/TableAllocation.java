package com.bookeat.availability.entity;
import com.bookeat.common.entity.BaseEntity;
import com.bookeat.restaurant.entity.Restaurant;
import com.bookeat.table.entity.RestaurantTable;
import com.bookeat.availability.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="table_allocations")
public class TableAllocation extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="restaurant_id",nullable=false)
    private Restaurant restaurant;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="table_id",nullable=false)
    private RestaurantTable table;
    @Column(nullable=false)
    private UUID referenceId;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private AllocationType referenceType;
    @Column(nullable=false)
    private Instant startsAt;
    @Column(nullable=false)
    private Instant diningEndsAt;
    @Column(nullable=false)
    private Instant endsAt;
    @Column(nullable=false)
    private int guestCount;
    @Column(nullable=false)
    private boolean active;
    @Column(nullable=true)
    private Instant releasedAt;
}
