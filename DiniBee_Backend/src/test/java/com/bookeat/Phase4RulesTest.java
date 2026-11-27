package com.bookeat;
import org.junit.jupiter.api.Test;
class Phase4RulesTest {
    @Test void tableCapacityAndOperationalStatuses(){Phase4RuleChecks.tableRules();}
    @Test void decimalMenuPricing(){Phase4RuleChecks.pricing();}
    @Test void intervalIndexMatchesLinearOracle(){Phase4RuleChecks.intervalIndex();}
    @Test void bookingWindowLeadTimeAndCapacity(){Phase4RuleChecks.bookingWindowAndCapacity();}
    @Test void timedAllocationsAndGrace(){Phase4RuleChecks.allocationsAndGrace();}
    @Test void overnightHoursAndDatedExceptions(){Phase4RuleChecks.overnightAndExceptions();}
    @Test void daylightSavingTransitions(){Phase4RuleChecks.daylightSaving();}
}
