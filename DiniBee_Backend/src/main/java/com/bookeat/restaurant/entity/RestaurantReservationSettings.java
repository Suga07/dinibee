package com.bookeat.restaurant.entity;
import com.bookeat.common.entity.BaseEntity;
import com.bookeat.owner.entity.RestaurantOwner;
import com.bookeat.restaurant.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;
import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="restaurant_reservation_settings")
public class RestaurantReservationSettings extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="restaurant_id",nullable=false,unique=true)
    private Restaurant restaurant;
    @Column(nullable=false)
    private boolean reservationEnabled;
    @Column(nullable=false)
    private int minimumGuests;
    @Column(nullable=false)
    private int maximumGuestsPerBooking;
    @Column(nullable=false)
    private int slotDurationMinutes;
    @Column(nullable=false)
    private int reservationDurationMinutes;
    @Column(nullable=false)
    private int advanceBookingDays;
    @Column(nullable=false)
    private int minimumAdvanceMinutes;
    @Column(nullable=false)
    private int gracePeriodMinutes;
    @Column(nullable=false)
    private boolean cancellationAllowed;
    @Column(nullable=false)
    private int cancellationBeforeMinutes;
    @Column(nullable=false)
    private boolean autoConfirm;
    @Column(nullable=false)
    private boolean specialRequestAllowed;
    @Column(nullable=false)
    private boolean preOrderAllowed;
    @Column(nullable=false)
    private boolean gpsCheckInEnabled;
    @Column(nullable=false)
    private boolean qrCheckInEnabled;
    @Column(nullable=false)
    private int gpsCheckInRadiusMeters;
}
