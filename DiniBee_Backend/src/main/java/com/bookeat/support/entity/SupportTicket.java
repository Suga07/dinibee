package com.bookeat.support.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="support_tickets") @Getter @Setter @NoArgsConstructor
public class SupportTicket extends BaseEntity {
    @Column(nullable=false,length=40) private String ticketNumber;
    @Column(nullable=false) private UUID createdByUser;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=20) private com.bookeat.support.enums.CreatorType creatorType;
    @Column(nullable=false,length=80) private String category;
    @Column(nullable=false,length=160) private String subject;
    @Column(nullable=false,length=5000) private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=20) private com.bookeat.support.enums.SupportPriority priority;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30) private com.bookeat.support.enums.SupportStatus status;
    @Column(nullable=true) private UUID assignedAdmin;
    @Column(nullable=true) private Instant resolvedAt;
}
