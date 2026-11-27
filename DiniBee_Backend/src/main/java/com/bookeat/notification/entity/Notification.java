package com.bookeat.notification.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="notifications") @Getter @Setter @NoArgsConstructor
public class Notification extends BaseEntity {
    @Column(nullable=false) private UUID userId;
    @Column(nullable=false,length=160) private String title;
    @Column(nullable=false,length=2000) private String message;
    @Column(nullable=false,length=60) private String notificationType;
    @Column(nullable=false,length=40) private String referenceType;
    @Column(nullable=false) private UUID referenceId;
    @Column(nullable=false) private boolean readFlag;
    @Column(nullable=false,length=200) private String deduplicationKey;
}
