package com.bookeat.admin.entity;

import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "promotions")
public class Promotion extends BaseEntity {
    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 20)
    private String kind;

    @Column(nullable = true)
    private UUID restaurantId;

    @Column(nullable = true, length = 20)
    private String serviceType;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal minimumAmount;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal maximumDiscount;

    @Column(nullable = false)
    private int maximumUses;

    @Column(nullable = false)
    private int uses;

    @Column(nullable = false)
    private Instant effectiveFrom;

    @Column(nullable = false)
    private Instant effectiveUntil;

    @Column(nullable = false)
    private boolean active;

    public Promotion() {}

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public UUID getRestaurantId() { return restaurantId; }
    public void setRestaurantId(UUID restaurantId) { this.restaurantId = restaurantId; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public BigDecimal getMinimumAmount() { return minimumAmount; }
    public void setMinimumAmount(BigDecimal minimumAmount) { this.minimumAmount = minimumAmount; }

    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }

    public BigDecimal getMaximumDiscount() { return maximumDiscount; }
    public void setMaximumDiscount(BigDecimal maximumDiscount) { this.maximumDiscount = maximumDiscount; }

    public int getMaximumUses() { return maximumUses; }
    public void setMaximumUses(int maximumUses) { this.maximumUses = maximumUses; }

    public int getUses() { return uses; }
    public void setUses(int uses) { this.uses = uses; }

    public Instant getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(Instant effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public Instant getEffectiveUntil() { return effectiveUntil; }
    public void setEffectiveUntil(Instant effectiveUntil) { this.effectiveUntil = effectiveUntil; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
