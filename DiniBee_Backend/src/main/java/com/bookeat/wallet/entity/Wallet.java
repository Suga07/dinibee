package com.bookeat.wallet.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="wallets") @Getter @Setter @NoArgsConstructor
public class Wallet extends BaseEntity {
    @Column(nullable=false) private long rewardDebt;
    @Column(nullable=false) private UUID customerId;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal balance;
    @Column(nullable=false) private long rewardPoints;
}
