package com.bookeat.diniticket.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="dini_tickets") @Getter @Setter @NoArgsConstructor
public class DiniTicket extends BaseEntity {
    @Column(nullable=false,length=50) private String ticketNumber;
    @Column(nullable=false) private long queueSequence;
    @Column(nullable=false) private UUID restaurantId;
    @Column(nullable=false) private UUID customerId;
    @Column(nullable=false) private int guestCount;
    @Column(nullable=false) private int queuePosition;
    @Column(nullable=false) private int estimatedWaitMinutes;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30) private com.bookeat.diniticket.enums.TicketStatus status;
    @Column(nullable=false) private Instant issuedAt;
    @Column(nullable=true) private Instant calledAt;
    @Column(nullable=true) private Instant arrivedAt;
    @Column(nullable=true) private Instant seatedAt;
    @Column(nullable=true) private Instant completedAt;
    @Column(nullable=false) private Instant expiresAt;
    @Column(nullable=true) private UUID tableId;
    @Column(nullable=true) private UUID allocationId;
}
