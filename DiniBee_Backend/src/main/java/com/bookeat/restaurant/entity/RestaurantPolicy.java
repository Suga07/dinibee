package com.bookeat.restaurant.entity;
import com.bookeat.common.entity.BaseEntity;
import com.bookeat.owner.entity.RestaurantOwner;
import com.bookeat.restaurant.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;
import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="restaurant_policies")
public class RestaurantPolicy extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="restaurant_id",nullable=false,unique=true)
    private Restaurant restaurant;
    @Column(nullable=false,length=3000)
    private String cancellationPolicy;
    @Column(nullable=false,length=3000)
    private String refundPolicy;
    @Column(nullable=true,length=3000)
    private String childPolicy;
    @Column(nullable=true,length=3000)
    private String petPolicy;
    @Column(nullable=true,length=3000)
    private String dressCode;
    @Column(nullable=true,length=3000)
    private String additionalPolicy;
}
