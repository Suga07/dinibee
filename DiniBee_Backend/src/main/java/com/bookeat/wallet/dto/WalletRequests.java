package com.bookeat.wallet.dto;
import jakarta.validation.constraints.*;import com.bookeat.wallet.enums.RewardRuleType;import java.math.BigDecimal;import java.util.UUID;import java.time.Instant;
public final class WalletRequests {
 private WalletRequests(){}
 public record Redemption(@Min(1) long points){}
 public record Adjustment(@NotNull @Digits(integer=12,fraction=2) BigDecimal amount,long points,@NotBlank @Size(max=1000) String reason){}
 public record Claim(@NotNull RewardRuleType ruleType,@NotNull UUID referenceId){}
 public record Rule(@NotNull RewardRuleType ruleType,@NotNull @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal minimumSpend,@Min(0) @Max(1000000000) long points,@NotNull @DecimalMin("0") @Digits(integer=8,fraction=2) BigDecimal conversionValue,@NotNull Boolean active,@NotNull Instant effectiveFrom,Instant effectiveUntil){}
}
