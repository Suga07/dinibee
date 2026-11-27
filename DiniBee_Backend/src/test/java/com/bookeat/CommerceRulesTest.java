package com.bookeat;
import org.junit.jupiter.api.Test;
class CommerceRulesTest {
    @Test void reservationTransitionsAndCancellationDeadlines(){CommerceRuleChecks.reservationLifecycle();}
    @Test void boundedRecurrenceAcrossMonthsAndDst(){CommerceRuleChecks.recurrence();}
    @Test void taxAndExactCentSplits(){CommerceRuleChecks.totalsAndSplits();}
    @Test void kitchenAndPickupTransitions(){CommerceRuleChecks.orderLifecycle();}
    @Test void queueLifecycleAndEstimates(){CommerceRuleChecks.ticketLifecycle();}
    @Test void locationDistanceAndEstimateBounds(){CommerceRuleChecks.geolocation();}
}
