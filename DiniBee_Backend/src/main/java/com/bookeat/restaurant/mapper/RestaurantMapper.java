package com.bookeat.restaurant.mapper;
import com.bookeat.restaurant.entity.*;
import com.bookeat.restaurant.enums.*;
import com.bookeat.restaurant.repository.*;
import com.bookeat.restaurant.dto.RestaurantResponses.*;
import com.bookeat.restaurant.service.*;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.*;
@Component @RequiredArgsConstructor
public class RestaurantMapper {
    private final RestaurantServiceActivationRepository restaurantServiceActivation;
    private final RestaurantOperatingHourRepository restaurantOperatingHour;
    private final RestaurantSpecialScheduleRepository restaurantSpecialSchedule;
    private final RestaurantDocumentRepository restaurantDocument;
    private final RestaurantReservationSettingsRepository restaurantReservationSettings;
    private final RestaurantTakeawaySettingsRepository restaurantTakeawaySettings;
    private final DiniTicketSettingsRepository diniTicketSettings;
    private final RestaurantPolicyRepository restaurantPolicy;
    private final RestaurantSettlementProfileRepository restaurantSettlementProfile;
    private final RestaurantTableSetupRepository restaurantTableSetup;
    private final RestaurantMenuSetupRepository restaurantMenuSetup;
    private final RestaurantTeamAccessRepository restaurantTeamAccess;
    private final RestaurantTeamDraftRepository restaurantTeamDraft;
    private final OnboardingProgressService progress;
    public Set<ServiceType> enabledServices(Restaurant r) {
        var enabled=EnumSet.noneOf(ServiceType.class);
        restaurantServiceActivation.findByRestaurantIdOrderByCreatedAtAscIdAsc(r.getId()).stream()
            .filter(s->WorkflowRules.effectiveService(r.getApprovalStatus(),r.getStatus(),s.isRequested(),s.isAuthorized(),s.isEnabled()))
            .forEach(s->enabled.add(s.getServiceType())); return enabled;
    }
    public Set<ServiceType> requestedServices(Restaurant r) {
        var requested=EnumSet.noneOf(ServiceType.class);
        restaurantServiceActivation.findByRestaurantIdOrderByCreatedAtAscIdAsc(r.getId()).stream()
            .filter(RestaurantServiceActivation::isRequested)
            .forEach(s->requested.add(s.getServiceType())); return requested;
    }
    public Summary summary(Restaurant r) {
        String ownerName = "";
        try {
            var user = r.getOwner().getUser();
            ownerName = (user.getFirstName() + " " + user.getLastName()).trim();
        } catch (Exception ignored) {}
        return new Summary(r.getId(), r.getVersion(), r.getRestaurantName(), r.getCity(),
                r.getApprovalStatus(), r.getStatus(), enabledServices(r), requestedServices(r),
                r.getSubmittedAt(), r.getCreatedAt(),
                ownerName, r.getPhone(), r.getEmail());
    }
    public Detail detail(Restaurant r,boolean admin) {
        // LOGO/COVER_IMAGE documents are the durable source of truth. This also
        // recovers restaurants created by older builds whose URL columns were stale.
        var liveDocs=restaurantDocument.findByRestaurantIdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(r.getId());
        String effectiveLogo=liveDocs.stream().filter(d->d.getDocumentType()==DocumentType.LOGO)
            .max(java.util.Comparator.comparing(RestaurantDocument::getCreatedAt).thenComparing(RestaurantDocument::getId))
            .map(d->"/api/v1/public/restaurants/"+r.getId()+"/media/"+d.getId()).orElse(r.getLogoUrl());
        String effectiveCover=liveDocs.stream().filter(d->d.getDocumentType()==DocumentType.COVER_IMAGE)
            .max(java.util.Comparator.comparing(RestaurantDocument::getCreatedAt).thenComparing(RestaurantDocument::getId))
            .map(d->"/api/v1/public/restaurants/"+r.getId()+"/media/"+d.getId()).orElse(r.getCoverImageUrl());
        return new Detail(r.getId(),
        r.getVersion(),
        r.getOwner().getId(),
        r.getRestaurantName(),
        r.getDescription(),
        r.getRestaurantType(),
        r.getEmail(),
        r.getPhone(),
        r.getAlternatePhone(),
        r.getAddressLine1(),
        r.getAddressLine2(),
        r.getCity(),
        r.getState(),
        r.getPostalCode(),
        r.getCountry(),
        r.getLatitude(),
        r.getLongitude(),
        r.getTimezone(),
        r.getBusinessRegistrationNumber(),
        r.getGstNumber(),
        r.getFssaiNumber(),
        effectiveLogo,
        effectiveCover,
        r.getApprovalStatus(),
        r.getStatus(),
        r.getSubmittedAt(),
        r.getApprovedAt(),
        r.getApprovedBy(),
        r.getRejectionReason(),
        r.getChangeRequestReason(),
        r.getCreatedAt(),
        r.getUpdatedAt(),
        restaurantServiceActivation.findByRestaurantIdOrderByCreatedAtAscIdAsc(r.getId()).stream().map(this::map).toList(),
        restaurantOperatingHour.findByRestaurantIdOrderByCreatedAtAscIdAsc(r.getId()).stream().map(this::map).toList(),
        restaurantSpecialSchedule.findByRestaurantIdOrderByCreatedAtAscIdAsc(r.getId()).stream().map(this::map).toList(),
        restaurantDocument.findByRestaurantIdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(r.getId()).stream().map(d->map(d,admin)).toList(),
        restaurantReservationSettings.findByRestaurantId(r.getId()).map(this::map).orElse(null),
        restaurantTakeawaySettings.findByRestaurantId(r.getId()).map(this::map).orElse(null),
        diniTicketSettings.findByRestaurantId(r.getId()).map(this::map).orElse(null),
        restaurantPolicy.findByRestaurantId(r.getId()).map(this::map).orElse(null),
        restaurantSettlementProfile.findByRestaurantId(r.getId()).map(this::map).orElse(null),
        restaurantTableSetup.findByRestaurantId(r.getId()).map(this::map).orElse(null),
        restaurantMenuSetup.findByRestaurantId(r.getId()).map(this::map).orElse(null),
        restaurantTeamAccess.findByRestaurantId(r.getId()).map(this::map).orElse(null),
        restaurantTeamDraft.findByRestaurantIdOrderByCreatedAtAscIdAsc(r.getId()).stream().map(this::map).toList(),
        progress.calculate(r), r.getOnboardingForm());}
    public ServiceActivationView map(RestaurantServiceActivation e) {return new ServiceActivationView(e.getId(), e.getServiceType(), e.isRequested(), e.isAuthorized(), e.isEnabled(), e.getActivatedAt(), e.getDisabledAt());}
    public OperatingHourView map(RestaurantOperatingHour e) {return new OperatingHourView(e.getId(), e.getDayOfWeek(), e.getOpeningTime(), e.getClosingTime(), e.isClosed(), e.isClosesNextDay());}
    public SpecialScheduleView map(RestaurantSpecialSchedule e) {return new SpecialScheduleView(e.getId(), e.getDate(), e.isClosed(), e.getOpeningTime(), e.getClosingTime(), e.isClosesNextDay(), e.getReason());}
    public DocumentView map(RestaurantDocument e,boolean admin) {return new DocumentView(e.getId(), e.getDocumentType(), admin?e.getFileUrl().replace("/owner/","/admin/"):e.getFileUrl(), e.getOriginalFilename(), e.getContentType(), e.getSizeBytes(), e.getSha256(), e.getVerificationStatus(), e.getVerifiedBy(), e.getVerifiedAt(), e.getRejectionReason(), mask(e.getDocumentNumber()));}
    public ReservationSettingsView map(RestaurantReservationSettings e) {return new ReservationSettingsView(e.getId(), e.isReservationEnabled(), e.getMinimumGuests(), e.getMaximumGuestsPerBooking(), e.getSlotDurationMinutes(), e.getReservationDurationMinutes(), e.getAdvanceBookingDays(), e.getMinimumAdvanceMinutes(), e.getGracePeriodMinutes(), e.isCancellationAllowed(), e.getCancellationBeforeMinutes(), e.isAutoConfirm(), e.isSpecialRequestAllowed(), e.isPreOrderAllowed(), e.isGpsCheckInEnabled(), e.isQrCheckInEnabled(), e.getGpsCheckInRadiusMeters());}
    public TakeawaySettingsView map(RestaurantTakeawaySettings e) {return new TakeawaySettingsView(e.getId(), e.isEnabled(), e.isRegularTakeawayEnabled(), e.isOnTheWayEnabled(), e.getMinimumOrderAmount(), e.getAveragePreparationMinutes(), e.getMaximumAdvanceOrderMinutes(), e.isAcceptCash(), e.isAcceptOnlinePayment());}
    public DiniTicketSettingsView map(DiniTicketSettings e) {return new DiniTicketSettingsView(e.getId(), e.isEnabled(), e.getMaximumQueueSize(), e.getDefaultEstimatedMinutesPerParty(), e.isAllowPreOrder(), e.isAllowRemoteJoin(), e.getTicketExpiryMinutes(), e.isAutoExpireTickets());}
    public PolicyView map(RestaurantPolicy e) {return new PolicyView(e.getId(), e.getCancellationPolicy(), e.getRefundPolicy(), e.getChildPolicy(), e.getPetPolicy(), e.getDressCode(), e.getAdditionalPolicy());}
    public SettlementProfileView map(RestaurantSettlementProfile e) {return new SettlementProfileView(e.getId(), e.getAccountHolderName(), e.getBankName(), e.getIfscCode(), e.getUpiId(), e.getTaxName(), e.getGstNumber(), e.isVerified(), e.getVerifiedBy(), e.getVerifiedAt(), e.getRejectionReason(), "****"+e.getAccountNumberLast4());}
    public TableSetupView map(RestaurantTableSetup e) {return new TableSetupView(e.getId(), e.getPlannedTableCount(), e.getPlannedSeatCount(), e.getSectionDescription());}
    public MenuSetupView map(RestaurantMenuSetup e) {return new MenuSetupView(e.getId(), e.getMode(), e.getExternalMenuUrl(), e.getNotes(), e.isAcknowledged());}
    public TeamAccessView map(RestaurantTeamAccess e) {return new TeamAccessView(e.getId(), e.getMode(), e.isAcknowledged());}
    public TeamDraftView map(RestaurantTeamDraft e) {return new TeamDraftView(e.getId(), e.getEmail(), e.getRole());}
    public ApprovalHistoryView map(RestaurantApprovalHistory e) {return new ApprovalHistoryView(e.getId(), e.getActorId(), e.getReferenceId(), e.getCreatedAt(), e.getAction(), e.getFromStatus(), e.getToStatus(), e.getReason(), e.getServiceType());}
    private static String mask(String value){return value==null?null:"****"+value.substring(Math.max(0,value.length()-4));}
}
