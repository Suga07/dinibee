package com.bookeat.reservation.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="group_bookings") @Getter @Setter @NoArgsConstructor
public class GroupBooking extends BaseEntity {
    @Column(nullable=false) private long changeSequence;
    @Column(nullable=false) private UUID reservationId;
    @Column(nullable=false) private UUID hostCustomerId;
    @Column(nullable=false) private UUID restaurantId;
    @Column(nullable=true,length=64) private String inviteHash;
    @Column(nullable=true) private Instant inviteExpiresAt;
    @Column(nullable=false) private int memberLimit;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30) private com.bookeat.reservation.enums.GroupStatus status;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30) private com.bookeat.reservation.enums.GroupPaymentMode paymentMode;
}
