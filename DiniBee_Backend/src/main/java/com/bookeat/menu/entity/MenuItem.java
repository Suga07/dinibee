package com.bookeat.menu.entity;
import com.bookeat.common.entity.BaseEntity;
import com.bookeat.restaurant.entity.Restaurant;
import com.bookeat.menu.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="menu_items")
public class MenuItem extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="restaurant_id",nullable=false)
    private Restaurant restaurant;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="category_id",nullable=false)
    private MenuCategory category;
    @Column(nullable=false,length=160)
    private String name;
    @Column(nullable=true,length=3000)
    private String description;
    @Column(nullable=false,precision=12,scale=2)
    private BigDecimal basePrice;
    @Column(nullable=true,precision=12,scale=2)
    private BigDecimal discountPrice;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private FoodType foodType;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private SpiceLevel spiceLevel;
    @Column(nullable=true,length=2048)
    private String imageUrl;
    @Column(nullable=false)
    private boolean available;
    @Column(nullable=false)
    private boolean preOrderAvailable;
    @Column(nullable=false)
    private boolean takeawayAvailable;
    @Column(nullable=false)
    private boolean diniTicketPreOrderAvailable;
    @Column(nullable=false)
    private int preparationTimeMinutes;
    @Column(nullable=false)
    private boolean featured;
    @Column(nullable=false)
    private int displayOrder;
    @Column(nullable=true)
    private Instant deletedAt;
}
