package com.bookeat.restaurant.entity;
import com.bookeat.common.entity.BaseEntity;
import com.bookeat.owner.entity.RestaurantOwner;
import com.bookeat.restaurant.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;
import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="restaurant_menu_setup")
public class RestaurantMenuSetup extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="restaurant_id",nullable=false,unique=true)
    private Restaurant restaurant;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private MenuSetupMode mode;
    @Column(nullable=true,length=2048)
    private String externalMenuUrl;
    @Column(nullable=true,length=1000)
    private String notes;
    @Column(nullable=false)
    private boolean acknowledged;
}
