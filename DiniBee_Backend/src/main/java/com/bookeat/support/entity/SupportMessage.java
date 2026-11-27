package com.bookeat.support.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="support_messages") @Getter @Setter @NoArgsConstructor
public class SupportMessage extends BaseEntity {
    @Column(nullable=false) private UUID ticketId;
    @Column(nullable=false) private UUID senderUser;
    @Column(nullable=false,length=5000) private String message;
    @Column(nullable=true,length=1000) private String attachmentUrl;
}
