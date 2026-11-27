package com.bookeat.wallet.dto;
import com.bookeat.wallet.enums.*;import java.util.UUID;import java.math.BigDecimal;import java.time.Instant;
public final class WalletResponses {
 private WalletResponses(){}
 public record Balance(UUID id,long version,BigDecimal balance,long rewardPoints,long rewardDebt,String currency){}
 public record Entry(UUID id,WalletTransactionType transactionType,String referenceType,UUID referenceId,BigDecimal amount,long points,BigDecimal balanceBefore,BigDecimal balanceAfter,long pointsBefore,long pointsAfter,long debtBefore,long debtAfter,String reason,Instant createdAt){}
 public record Redemption(long points,BigDecimal amount,String currency,UUID ruleId){}
 public record Rule(UUID id,long version,RewardRuleType ruleType,BigDecimal minimumSpend,long points,BigDecimal conversionValue,Instant effectiveFrom,Instant effectiveUntil,boolean active){}
}
