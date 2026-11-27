package com.bookeat.wallet.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="reward_rules") @Getter @Setter @NoArgsConstructor
public class RewardRule extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30) private com.bookeat.wallet.enums.RewardRuleType ruleType;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal minimumSpend;
    @Column(nullable=false) private long points;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal conversionValue;
    @Column(nullable=false) private boolean active;
    @Column(nullable=false) private Instant effectiveFrom;
    @Column(nullable=true) private Instant effectiveUntil;
}
