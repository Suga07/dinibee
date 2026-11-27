package com.bookeat;
import com.bookeat.restaurant.enums.*;
import com.bookeat.restaurant.service.WorkflowRules;
import com.bookeat.restaurant.security.SettlementCipher;
import com.bookeat.restaurant.storage.DocumentContentRules;
import org.junit.jupiter.api.Test;
import java.time.LocalTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class OnboardingRulesTest {
    @Test void draftAndSuspendedCannotBeApprovedDirectly() {
        assertThat(WorkflowRules.permits(ApprovalStatus.DRAFT,ReviewAction.APPROVE)).isFalse();
        assertThat(WorkflowRules.permits(ApprovalStatus.SUBMITTED,ReviewAction.APPROVE)).isFalse();
        assertThat(WorkflowRules.permits(ApprovalStatus.SUSPENDED,ReviewAction.APPROVE)).isFalse();
        assertThat(WorkflowRules.permits(ApprovalStatus.UNDER_REVIEW,ReviewAction.APPROVE)).isTrue();
    }
    @Test void ownerCannotEditReviewedOrActiveApplications() {
        for(var status:List.of(ApprovalStatus.SUBMITTED,ApprovalStatus.UNDER_REVIEW,ApprovalStatus.APPROVED,ApprovalStatus.SUSPENDED))
            assertThat(WorkflowRules.ownerEditable(status)).isFalse();
        for(var status:List.of(ApprovalStatus.DRAFT,ApprovalStatus.REJECTED,ApprovalStatus.CHANGES_REQUESTED))
            assertThat(WorkflowRules.ownerEditable(status)).isTrue();
    }
    @Test void requestedOrAuthorizedAloneDoesNotEnableService() {
        assertThat(WorkflowRules.effectiveService(ApprovalStatus.APPROVED,RestaurantStatus.ACTIVE,true,false,true)).isFalse();
        assertThat(WorkflowRules.effectiveService(ApprovalStatus.APPROVED,RestaurantStatus.ACTIVE,true,true,false)).isFalse();
        assertThat(WorkflowRules.effectiveService(ApprovalStatus.SUSPENDED,RestaurantStatus.SUSPENDED,true,true,true)).isFalse();
        assertThat(WorkflowRules.effectiveService(ApprovalStatus.APPROVED,RestaurantStatus.ACTIVE,true,true,true)).isTrue();
    }
    @Test void overnightHoursRequireExplicitFlagAndClosedDaysHaveNoTimes() {
        var open=LocalTime.of(20,0);var close=LocalTime.of(2,0);
        assertThat(WorkflowRules.validHours(false,open,close,false)).isFalse();
        assertThat(WorkflowRules.validHours(false,open,close,true)).isTrue();
        assertThat(WorkflowRules.validHours(true,open,close,true)).isFalse();
        assertThat(WorkflowRules.validHours(true,null,null,false)).isTrue();
        assertThat(WorkflowRules.validHours(false,open,open,false)).isFalse();
    }
    @Test void bankCipherIsRandomizedAndBoundToRestaurant() {
        var c=new SettlementCipher(Base64.getEncoder().encodeToString(new byte[32]));var id=UUID.randomUUID();
        var a=c.encrypt(id,"123456789012");var b=c.encrypt(id,"123456789012");
        assertThat(a).isNotEqualTo(b).doesNotContain("123456789012");
        assertThat(c.decrypt(id,a)).isEqualTo("123456789012");
        assertThatThrownBy(()->c.decrypt(UUID.randomUUID(),a)).isInstanceOf(IllegalStateException.class);
    }
    @Test void bankCipherRejectsTamperingAndWrongKeys() {
        var c=new SettlementCipher(Base64.getEncoder().encodeToString(new byte[32]));var id=UUID.randomUUID();var value=c.encrypt(id,"12345678");
        byte[] modified=Base64.getDecoder().decode(value.substring(3));modified[15]^=1;
        assertThatThrownBy(()->c.decrypt(id,"v1:"+Base64.getEncoder().encodeToString(modified))).isInstanceOf(IllegalStateException.class);
        byte[] other=new byte[32];Arrays.fill(other,(byte)1);
        assertThatThrownBy(()->new SettlementCipher(Base64.getEncoder().encodeToString(other)).decrypt(id,value)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(()->new SettlementCipher("short")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void uploadRulesRejectArbitraryContentOversizeAndTraversal() {
        assertThatThrownBy(()->DocumentContentRules.detect("<script>alert(1)</script>".getBytes())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->DocumentContentRules.detect(new byte[DocumentContentRules.MAX_BYTES+1])).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->DocumentContentRules.validateKey("../../credentials")).isInstanceOf(IllegalArgumentException.class);
        assertThat(DocumentContentRules.safeFilename("../../unsafe file\r\n.pdf")).isEqualTo("unsafe_file__.pdf");
        assertThat(DocumentContentRules.detect("%PDF-1.7 test".getBytes())).isEqualTo("application/pdf");
    }
}
