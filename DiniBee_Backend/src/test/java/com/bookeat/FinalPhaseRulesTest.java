package com.bookeat;
import org.junit.jupiter.api.Test;
class FinalPhaseRulesTest {
 @Test void diningArrivalAndRadius(){FinalPhaseRuleChecks.dining();}
 @Test void exactMoneyAndRefundLimits(){FinalPhaseRuleChecks.money();}
 @Test void paymentSignaturesRejectTampering(){FinalPhaseRuleChecks.signatures();}
 @Test void walletConservationAndRewardDebt(){FinalPhaseRuleChecks.wallet();}
 @Test void safeCsvAndWellFormedExcel()throws Exception{FinalPhaseRuleChecks.exports();}
}
