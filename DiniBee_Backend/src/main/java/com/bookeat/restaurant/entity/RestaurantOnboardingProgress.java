package com.bookeat.restaurant.entity;
import com.bookeat.common.entity.BaseEntity;
import com.bookeat.owner.entity.RestaurantOwner;
import com.bookeat.restaurant.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;
import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="restaurant_onboarding_progress")
public class RestaurantOnboardingProgress extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="restaurant_id",nullable=false,unique=true)
    private Restaurant restaurant;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private OnboardingStep currentStep;
    @Column(nullable=false)
    private boolean restaurantDetailsComplete;
    @Column(nullable=false)
    private boolean locationComplete;
    @Column(nullable=false)
    private boolean businessInfoComplete;
    @Column(nullable=false)
    private boolean hoursComplete;
    @Column(nullable=false)
    private boolean servicesComplete;
    @Column(nullable=false)
    private boolean tableSetupComplete;
    @Column(nullable=false)
    private boolean reservationSettingsComplete;
    @Column(nullable=false)
    private boolean takeawaySettingsComplete;
    @Column(nullable=false)
    private boolean diniticketSettingsComplete;
    @Column(nullable=false)
    private boolean menuSetupComplete;
    @Column(nullable=false)
    private boolean paymentDetailsComplete;
    @Column(nullable=false)
    private boolean policiesComplete;
    @Column(nullable=false)
    private boolean teamAccessComplete;
    @Column(nullable=false)
    private boolean submitted;
}
