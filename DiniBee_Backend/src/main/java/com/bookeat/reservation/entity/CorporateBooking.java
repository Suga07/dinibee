package com.bookeat.reservation.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="corporate_bookings") @Getter @Setter @NoArgsConstructor
public class CorporateBooking extends BaseEntity {
    @Column(nullable=false) private long changeSequence;
    @Column(nullable=false) private UUID corporateAccountId;
    @Column(nullable=false) private UUID reservationId;
    @Column(nullable=false) private UUID restaurantId;
    @Column(nullable=false) private UUID organizerCustomerId;
    @Column(nullable=false) private UUID seriesId;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal budgetLimit;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30) private com.bookeat.reservation.enums.BillingType billingType;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30) private com.bookeat.reservation.enums.RecurrenceType recurrenceType;
    @Column(nullable=false,length=300) private String recurrenceRule;
    @Column(nullable=false) private boolean invoiceRequired;
}
