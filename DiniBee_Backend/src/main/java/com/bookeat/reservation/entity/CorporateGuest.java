package com.bookeat.reservation.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="corporate_guests") @Getter @Setter @NoArgsConstructor
public class CorporateGuest extends BaseEntity {
    @Column(nullable=false) private UUID corporateBookingId;
    @Column(nullable=true) private UUID customerId;
    @Column(nullable=false,length=100) private String name;
    @Column(nullable=false,length=320) private String email;
    @Column(nullable=true,length=30) private String phone;
    @Column(nullable=true,length=80) private String employeeCode;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30) private com.bookeat.reservation.enums.GuestStatus status;
    @Column(nullable=false,length=64) private String inviteHash;
    @Column(nullable=false) private Instant inviteExpiresAt;
}
