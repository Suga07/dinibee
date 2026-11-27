package com.bookeat.table.entity;
import com.bookeat.common.entity.BaseEntity;
import com.bookeat.restaurant.entity.Restaurant;
import com.bookeat.table.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="restaurant_tables")
public class RestaurantTable extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="restaurant_id",nullable=false)
    private Restaurant restaurant;
    @Column(nullable=false,length=30)
    private String tableNumber;
    @Column(nullable=true,length=100)
    private String tableName;
    @Column(nullable=false)
    private int capacity;
    @Column(nullable=false)
    private int minimumGuests;
    @Column(nullable=false)
    private int maximumGuests;
    @Column(nullable=false)
    private int floor;
    @Column(nullable=true,length=100)
    private String section;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private TableShape shape;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private TableStatus status;
    @Column(nullable=false,precision=12,scale=2)
    private BigDecimal xPosition;
    @Column(nullable=false,precision=12,scale=2)
    private BigDecimal yPosition;
    @Column(nullable=false,precision=12,scale=2)
    private BigDecimal width;
    @Column(nullable=false,precision=12,scale=2)
    private BigDecimal height;
    @Column(nullable=false,precision=6,scale=2)
    private BigDecimal rotation;
    @Column(nullable=false)
    private boolean active;
    @Column(nullable=true)
    private Instant deletedAt;
}
