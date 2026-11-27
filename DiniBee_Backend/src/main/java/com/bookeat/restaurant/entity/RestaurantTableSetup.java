package com.bookeat.restaurant.entity;
import com.bookeat.common.entity.BaseEntity;
import com.bookeat.owner.entity.RestaurantOwner;
import com.bookeat.restaurant.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;
import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="restaurant_table_setup")
public class RestaurantTableSetup extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="restaurant_id",nullable=false,unique=true)
    private Restaurant restaurant;
    @Column(nullable=false)
    private int plannedTableCount;
    @Column(nullable=false)
    private int plannedSeatCount;
    @Column(nullable=true,length=500)
    private String sectionDescription;
}
