package com.bookeat.dining.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="dining_sessions") @Getter @Setter @NoArgsConstructor
public class DiningSession extends BaseEntity {
    @Column(nullable=true) private UUID reservationId;
    @Column(nullable=true) private UUID diniTicketId;
    @Column(nullable=false) private UUID customerId;
    @Column(nullable=false) private UUID restaurantId;
    @Column(nullable=true) private UUID tableId;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30) private com.bookeat.dining.enums.DiningStatus status;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30) private com.bookeat.dining.enums.CheckInMethod checkInMethod;
    @Column(nullable=true,precision=10,scale=7) private BigDecimal checkInLatitude;
    @Column(nullable=true,precision=10,scale=7) private BigDecimal checkInLongitude;
    @Column(nullable=false) private Instant checkedInAt;
    @Column(nullable=true) private Instant diningStartedAt;
    @Column(nullable=true) private Instant paymentPendingAt;
    @Column(nullable=true) private Instant paidAt;
    @Column(nullable=true) private Instant checkedOutAt;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal billTotal;
}
