package com.bookeat.wallet.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="wallet_transactions") @Getter @Setter @NoArgsConstructor
public class WalletTransaction extends BaseEntity {
    @Column(nullable=false) private long debtBefore;
    @Column(nullable=false) private long debtAfter;
    @Column(nullable=false) private UUID walletId;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=30) private com.bookeat.wallet.enums.WalletTransactionType transactionType;
    @Column(nullable=false,length=40) private String referenceType;
    @Column(nullable=false) private UUID referenceId;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal amount;
    @Column(nullable=false) private long points;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal balanceBefore;
    @Column(nullable=false,precision=18,scale=2) private BigDecimal balanceAfter;
    @Column(nullable=false) private long pointsBefore;
    @Column(nullable=false) private long pointsAfter;
    @Column(nullable=false,length=1000) private String reason;
    @Column(nullable=true) private UUID actorId;
}
