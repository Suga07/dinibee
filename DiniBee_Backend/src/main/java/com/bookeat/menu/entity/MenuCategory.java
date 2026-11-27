package com.bookeat.menu.entity;
import com.bookeat.common.entity.BaseEntity;
import com.bookeat.restaurant.entity.Restaurant;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="menu_categories")
public class MenuCategory extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="restaurant_id",nullable=false)
    private Restaurant restaurant;
    @Column(nullable=false,length=100)
    private String name;
    @Column(nullable=true,length=2000)
    private String description;
    @Column(name="image_url",length=2048)
    private String imageUrl;
    @Column(nullable=false)
    private int displayOrder;
    @Column(nullable=false)
    private boolean active;
    @Column(nullable=true)
    private Instant deletedAt;
}
