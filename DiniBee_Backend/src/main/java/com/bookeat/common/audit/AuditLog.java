package com.bookeat.common.audit;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity @Table(name="audit_logs") @Getter @Setter @NoArgsConstructor
public class AuditLog extends BaseEntity {
    @Column(length=1000) private String reason;
    private UUID actorId;
    @Column(nullable=false, length=80) private String action;
    @Column(nullable=false, length=60) private String entityType;
    @Column(nullable=false) private UUID entityId;
    @Column(columnDefinition="text") private String oldValue;
    @Column(columnDefinition="text") private String newValue;
    @Column(length=64) private String ipAddress;
}
