package com.bookeat.reservation.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="reservations") @Getter @Setter @NoArgsConstructor
public class Reservation extends BaseEntity {
    @Column(nullable=false,length=40) private String bookingReference;
    @Column(nullable=false) private UUID customerId;
    @Column(nullable=false) private UUID restaurantId;
    @Column(nullable=false) private UUID tableId;
    @Column(nullable=true) private UUID allocationId;
    @Column(nullable=false) private LocalDate reservationDate;
    @Column(nullable=false) private LocalTime startTime;
    @Column(nullable=false) private LocalTime endTime;
    @Column(nullable=false) private Instant startsAt;
    @Column(nullable=false) private Instant endsAt;
    @Column(nullable=false,length=60) private String timezone;
    @Column(nullable=false) private int guestCount;
    @Column(nullable=true,length=2000) private String specialRequest;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30) private com.bookeat.reservation.enums.BookingType bookingType;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30) private com.bookeat.reservation.enums.ReservationStatus status;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30) private com.bookeat.reservation.enums.BookingPaymentStatus paymentStatus;
    @Column(nullable=false) private boolean cancellationAllowed;
    @Column(nullable=false) private Instant cancellationDeadline;
    @Column(nullable=false) private Instant noShowAfter;
    @Column(nullable=true) private Instant checkedInAt;
    @Column(nullable=true) private Instant completedAt;
    @Column(nullable=true) private Instant cancelledAt;
    @Column(nullable=true,length=1000) private String cancellationReason;
}
