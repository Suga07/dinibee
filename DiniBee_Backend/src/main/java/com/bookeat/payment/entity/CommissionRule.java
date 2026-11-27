package com.bookeat.payment.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="commission_rules") @Getter @Setter @NoArgsConstructor
public class CommissionRule extends BaseEntity {
    @Column(nullable=false,length=20) private String serviceType;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=20) private com.bookeat.payment.enums.CommissionType commissionType;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal commissionValue;
    @Column(nullable=false) private Instant effectiveFrom;
    @Column(nullable=true) private Instant effectiveUntil;
    @Column(nullable=false) private boolean active;
}
