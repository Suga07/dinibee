package com.bookeat.reservation.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="reservation_status_history") @Getter @Setter @NoArgsConstructor
public class ReservationStatusHistory extends BaseEntity {
    @Column(nullable=false) private UUID reservationId;
    @Enumerated(EnumType.STRING)
    @Column(nullable=true,length=30) private com.bookeat.reservation.enums.ReservationStatus oldStatus;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30) private com.bookeat.reservation.enums.ReservationStatus newStatus;
    @Column(nullable=false) private UUID changedBy;
    @Column(nullable=true,length=1000) private String reason;
    @Column(nullable=false) private Instant changedAt;
}
