package com.bookeat.menu.entity;
import com.bookeat.common.entity.BaseEntity;
import com.bookeat.restaurant.entity.Restaurant;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="menu_addons")
public class MenuAddon extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="menu_item_id",nullable=false)
    private MenuItem menuItem;
    @Column(nullable=false,length=80)
    private String name;
    @Column(nullable=false,precision=12,scale=2)
    private BigDecimal price;
    @Column(nullable=false)
    private boolean active;
    @Column(nullable=true)
    private Instant deletedAt;
}
