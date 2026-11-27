package com.bookeat.reservation.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="group_members") @Getter @Setter @NoArgsConstructor
public class GroupMember extends BaseEntity {
    @Column(nullable=false) private UUID groupBookingId;
    @Column(nullable=false) private UUID customerId;
    @Column(nullable=false,length=100) private String displayName;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30) private com.bookeat.reservation.enums.MemberStatus memberStatus;
    @Column(nullable=true) private Instant joinedAt;
}
