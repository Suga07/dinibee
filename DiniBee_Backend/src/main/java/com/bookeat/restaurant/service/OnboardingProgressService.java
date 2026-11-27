package com.bookeat.restaurant.service;
import com.bookeat.restaurant.entity.*;
import com.bookeat.restaurant.enums.*;
import com.bookeat.restaurant.repository.*;
import com.bookeat.restaurant.dto.RestaurantResponses.Progress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.*;
@Service @RequiredArgsConstructor
public class OnboardingProgressService {
    private final RestaurantServiceActivationRepository restaurantServiceActivation;
    private final RestaurantOperatingHourRepository restaurantOperatingHour;
    private final RestaurantDocumentRepository restaurantDocument;
    private final RestaurantReservationSettingsRepository restaurantReservationSettings;
    private final RestaurantTakeawaySettingsRepository restaurantTakeawaySettings;
    private final DiniTicketSettingsRepository diniTicketSettings;
    private final RestaurantPolicyRepository restaurantPolicy;
    private final RestaurantSettlementProfileRepository restaurantSettlementProfile;
    private final RestaurantTableSetupRepository restaurantTableSetup;
    private final RestaurantMenuSetupRepository restaurantMenuSetup;
    private final RestaurantTeamAccessRepository restaurantTeamAccess;
    private final RestaurantOnboardingProgressRepository restaurantOnboardingProgress;
    @Value("${app.onboarding.required-documents:}")
    private String requiredDocsConfig;

    public List<DocumentType> requiredDocuments(Restaurant r) {
        if (requiredDocsConfig == null || requiredDocsConfig.isBlank()) {
            return Collections.emptyList();
        }
        var types = EnumSet.noneOf(DocumentType.class);
        for (String s : requiredDocsConfig.split(",")) {
            if (!s.isBlank()) {
                try {
                    types.add(DocumentType.valueOf(s.trim()));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return types.stream().sorted().toList();
    }
    public boolean documentMatches(Restaurant r,RestaurantDocument d) {
        return switch(d.getDocumentType()) {
            case FSSAI -> Objects.equals(r.getFssaiNumber(),d.getDocumentNumber());
            case GST -> Objects.equals(r.getGstNumber(),d.getDocumentNumber());
            case BUSINESS_REGISTRATION -> Objects.equals(r.getBusinessRegistrationNumber(),d.getDocumentNumber());
            default -> true;
        };
    }
    public boolean verificationComplete(Restaurant r) {
        var docs=restaurantDocument.findByRestaurantIdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(r.getId());
        return requiredDocuments(r).stream().allMatch(t->docs.stream().anyMatch(d->
            d.getDocumentType()==t && d.getVerificationStatus()==VerificationStatus.VERIFIED && documentMatches(r,d)))
            && restaurantSettlementProfile.findByRestaurantId(r.getId()).map(p->p.isVerified()
                && Objects.equals(p.getGstNumber(),r.getGstNumber())).orElse(false);
    }
    public boolean configured(Restaurant r,ServiceType type) {
        return switch(type) {
            case DINE_IN -> restaurantReservationSettings.findByRestaurantId(r.getId()).map(RestaurantReservationSettings::isReservationEnabled).orElse(false)
                && restaurantTableSetup.findByRestaurantId(r.getId()).isPresent();
            case TAKEAWAY -> restaurantTakeawaySettings.findByRestaurantId(r.getId()).map(RestaurantTakeawaySettings::isEnabled).orElse(false);
            case DINITICKET -> diniTicketSettings.findByRestaurantId(r.getId()).map(DiniTicketSettings::isEnabled).orElse(false)
                && restaurantTableSetup.findByRestaurantId(r.getId()).isPresent();
        };
    }
    public Progress calculate(Restaurant r) {
        UUID id=r.getId();
        var selected=EnumSet.noneOf(ServiceType.class);
        restaurantServiceActivation.findByRestaurantIdOrderByCreatedAtAscIdAsc(id).stream().filter(RestaurantServiceActivation::isRequested).forEach(s->selected.add(s.getServiceType()));
        var docs=restaurantDocument.findByRestaurantIdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(id);
        var flags=new LinkedHashMap<OnboardingStep,Boolean>();
        flags.put(OnboardingStep.RESTAURANT_DETAILS,present(r.getRestaurantName()) && present(r.getDescription()) && r.getRestaurantType()!=null && present(r.getEmail()) && present(r.getPhone()));
        flags.put(OnboardingStep.LOCATION,present(r.getAddressLine1()) && present(r.getCity()) && present(r.getState()) && present(r.getCountry()) && present(r.getPostalCode()) && r.getLatitude()!=null && r.getLongitude()!=null && present(r.getTimezone()));
        flags.put(OnboardingStep.BUSINESS_INFO,present(r.getBusinessRegistrationNumber()) && present(r.getFssaiNumber()) && requiredDocuments(r).stream().allMatch(t->docs.stream().anyMatch(d->d.getDocumentType()==t && d.getVerificationStatus()!=VerificationStatus.REJECTED && documentMatches(r,d))));
        var hours=restaurantOperatingHour.findByRestaurantIdOrderByCreatedAtAscIdAsc(id);
        flags.put(OnboardingStep.HOURS,hours.size()==7 && hours.stream().anyMatch(h->!h.isClosed()));
        flags.put(OnboardingStep.SERVICES,!selected.isEmpty());
        boolean needsTables=selected.contains(ServiceType.DINE_IN) || selected.contains(ServiceType.DINITICKET);
        flags.put(OnboardingStep.TABLE_SETUP,!needsTables || restaurantTableSetup.findByRestaurantId(id).isPresent());
        flags.put(OnboardingStep.RESERVATION_SETTINGS,!selected.contains(ServiceType.DINE_IN) || configured(r,ServiceType.DINE_IN));
        flags.put(OnboardingStep.TAKEAWAY_SETTINGS,!selected.contains(ServiceType.TAKEAWAY) || configured(r,ServiceType.TAKEAWAY));
        flags.put(OnboardingStep.DINITICKET_SETTINGS,!selected.contains(ServiceType.DINITICKET) || configured(r,ServiceType.DINITICKET));
        flags.put(OnboardingStep.MENU_SETUP,restaurantMenuSetup.findByRestaurantId(id).map(RestaurantMenuSetup::isAcknowledged).orElse(false));
        flags.put(OnboardingStep.PAYMENT_DETAILS,restaurantSettlementProfile.findByRestaurantId(id).map(p->p.getRejectionReason()==null && Objects.equals(p.getGstNumber(),r.getGstNumber())).orElse(false));
        flags.put(OnboardingStep.POLICIES,restaurantPolicy.findByRestaurantId(id).isPresent());
        flags.put(OnboardingStep.TEAM_ACCESS,restaurantTeamAccess.findByRestaurantId(id).map(RestaurantTeamAccess::isAcknowledged).orElse(false));
        var missing=flags.entrySet().stream().filter(e->!e.getValue()).map(Map.Entry::getKey).toList();
        var na=new ArrayList<OnboardingStep>();
        if(!needsTables) na.add(OnboardingStep.TABLE_SETUP);
        if(!selected.contains(ServiceType.DINE_IN)) na.add(OnboardingStep.RESERVATION_SETTINGS);
        if(!selected.contains(ServiceType.TAKEAWAY)) na.add(OnboardingStep.TAKEAWAY_SETTINGS);
        if(!selected.contains(ServiceType.DINITICKET)) na.add(OnboardingStep.DINITICKET_SETTINGS);
        boolean submitted=!WorkflowRules.ownerEditable(r.getApprovalStatus());
        return new Progress(missing.isEmpty()?OnboardingStep.REVIEW:missing.get(0),
            flags.get(OnboardingStep.RESTAURANT_DETAILS), flags.get(OnboardingStep.LOCATION), flags.get(OnboardingStep.BUSINESS_INFO), flags.get(OnboardingStep.HOURS), flags.get(OnboardingStep.SERVICES), flags.get(OnboardingStep.TABLE_SETUP), flags.get(OnboardingStep.RESERVATION_SETTINGS), flags.get(OnboardingStep.TAKEAWAY_SETTINGS), flags.get(OnboardingStep.DINITICKET_SETTINGS), flags.get(OnboardingStep.MENU_SETUP), flags.get(OnboardingStep.PAYMENT_DETAILS), flags.get(OnboardingStep.POLICIES), flags.get(OnboardingStep.TEAM_ACCESS),
            submitted,missing.isEmpty(),missing,List.copyOf(na),requiredDocuments(r));
    }
    public Progress refresh(Restaurant r) {
        var result=calculate(r);
        var p=restaurantOnboardingProgress.findByRestaurantId(r.getId()).orElseGet(RestaurantOnboardingProgress::new);
        p.setRestaurant(r); p.setCurrentStep(result.currentStep()); p.setSubmitted(result.submitted());
        p.setRestaurantDetailsComplete(result.restaurantDetailsComplete()); p.setLocationComplete(result.locationComplete()); p.setBusinessInfoComplete(result.businessInfoComplete()); p.setHoursComplete(result.hoursComplete()); p.setServicesComplete(result.servicesComplete()); p.setTableSetupComplete(result.tableSetupComplete()); p.setReservationSettingsComplete(result.reservationSettingsComplete()); p.setTakeawaySettingsComplete(result.takeawaySettingsComplete()); p.setDiniticketSettingsComplete(result.diniticketSettingsComplete()); p.setMenuSetupComplete(result.menuSetupComplete()); p.setPaymentDetailsComplete(result.paymentDetailsComplete()); p.setPoliciesComplete(result.policiesComplete()); p.setTeamAccessComplete(result.teamAccessComplete());
        restaurantOnboardingProgress.save(p); return result;
    }
    private static boolean present(String s){return s!=null && !s.isBlank();}
}
