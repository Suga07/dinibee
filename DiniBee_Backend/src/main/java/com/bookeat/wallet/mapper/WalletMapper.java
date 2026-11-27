package com.bookeat.wallet.mapper;
import com.bookeat.wallet.entity.*;import com.bookeat.wallet.dto.WalletResponses.*;import org.springframework.stereotype.Component;
@Component public class WalletMapper {
 public Balance balance(Wallet w){return new Balance(w.getId(),w.getVersion(),w.getBalance(),w.getRewardPoints(),w.getRewardDebt(),"INR");}
 public Entry entry(WalletTransaction t){return new Entry(t.getId(),t.getTransactionType(),t.getReferenceType(),t.getReferenceId(),t.getAmount(),t.getPoints(),t.getBalanceBefore(),t.getBalanceAfter(),t.getPointsBefore(),t.getPointsAfter(),t.getDebtBefore(),t.getDebtAfter(),t.getReason(),t.getCreatedAt());}
 public Rule rule(RewardRule r){return new Rule(r.getId(),r.getVersion(),r.getRuleType(),r.getMinimumSpend(),r.getPoints(),r.getConversionValue(),r.getEffectiveFrom(),r.getEffectiveUntil(),r.isActive());}
}
