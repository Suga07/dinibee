package com.bookeat.restaurant.dto;
import com.bookeat.restaurant.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
public final class RestaurantResponses {
 private RestaurantResponses() {}
    public record ServiceActivationView(UUID id, ServiceType serviceType, boolean requested, boolean authorized, boolean enabled, Instant activatedAt, Instant disabledAt) {}
    public record OperatingHourView(UUID id, DayOfWeek dayOfWeek, LocalTime openingTime, LocalTime closingTime, boolean closed, boolean closesNextDay) {}
    public record SpecialScheduleView(UUID id, LocalDate date, boolean closed, LocalTime openingTime, LocalTime closingTime, boolean closesNextDay, String reason) {}
    public record DocumentView(UUID id, DocumentType documentType, String fileUrl, String originalFilename, String contentType, long sizeBytes, String sha256, VerificationStatus verificationStatus, UUID verifiedBy, Instant verifiedAt, String rejectionReason, String maskedDocumentNumber) {}
    public record ReservationSettingsView(UUID id, boolean reservationEnabled, int minimumGuests, int maximumGuestsPerBooking, int slotDurationMinutes, int reservationDurationMinutes, int advanceBookingDays, int minimumAdvanceMinutes, int gracePeriodMinutes, boolean cancellationAllowed, int cancellationBeforeMinutes, boolean autoConfirm, boolean specialRequestAllowed, boolean preOrderAllowed, boolean gpsCheckInEnabled, boolean qrCheckInEnabled, int gpsCheckInRadiusMeters) {}
    public record TakeawaySettingsView(UUID id, boolean enabled, boolean regularTakeawayEnabled, boolean onTheWayEnabled, BigDecimal minimumOrderAmount, int averagePreparationMinutes, int maximumAdvanceOrderMinutes, boolean acceptCash, boolean acceptOnlinePayment) {}
    public record DiniTicketSettingsView(UUID id, boolean enabled, int maximumQueueSize, int defaultEstimatedMinutesPerParty, boolean allowPreOrder, boolean allowRemoteJoin, int ticketExpiryMinutes, boolean autoExpireTickets) {}
    public record PolicyView(UUID id, String cancellationPolicy, String refundPolicy, String childPolicy, String petPolicy, String dressCode, String additionalPolicy) {}
    public record SettlementProfileView(UUID id, String accountHolderName, String bankName, String ifscCode, String upiId, String taxName, String gstNumber, boolean verified, UUID verifiedBy, Instant verifiedAt, String rejectionReason, String maskedAccountNumber) {}
    public record TableSetupView(UUID id, int plannedTableCount, int plannedSeatCount, String sectionDescription) {}
    public record MenuSetupView(UUID id, MenuSetupMode mode, String externalMenuUrl, String notes, boolean acknowledged) {}
    public record TeamAccessView(UUID id, TeamAccessMode mode, boolean acknowledged) {}
    public record TeamDraftView(UUID id, String email, TeamRole role) {}
    public record OnboardingProgressView(UUID id, OnboardingStep currentStep, boolean restaurantDetailsComplete, boolean locationComplete, boolean businessInfoComplete, boolean hoursComplete, boolean servicesComplete, boolean tableSetupComplete, boolean reservationSettingsComplete, boolean takeawaySettingsComplete, boolean diniticketSettingsComplete, boolean menuSetupComplete, boolean paymentDetailsComplete, boolean policiesComplete, boolean teamAccessComplete, boolean submitted) {}
    public record ApprovalHistoryView(UUID id, UUID actorId, UUID referenceId, Instant createdAt, ReviewAction action, ApprovalStatus fromStatus, ApprovalStatus toStatus, String reason, ServiceType serviceType) {}
    public record Progress(OnboardingStep currentStep, boolean restaurantDetailsComplete, boolean locationComplete, boolean businessInfoComplete, boolean hoursComplete, boolean servicesComplete, boolean tableSetupComplete, boolean reservationSettingsComplete, boolean takeawaySettingsComplete, boolean diniticketSettingsComplete, boolean menuSetupComplete, boolean paymentDetailsComplete, boolean policiesComplete, boolean teamAccessComplete, boolean submitted, boolean complete, List<OnboardingStep> missingSteps, List<OnboardingStep> notApplicableSteps, List<DocumentType> requiredDocuments) {}
    public record Summary(UUID id, long version, String restaurantName, String city, ApprovalStatus approvalStatus, RestaurantStatus status, Set<ServiceType> enabledServices, Set<ServiceType> requestedServices, Instant submittedAt, Instant createdAt, String ownerName, String phone, String email) {}
    public record Owner(UUID id, UUID userId, String firstName, String lastName, String email) {}
    public record DashboardContext(Owner owner, Summary restaurant, ApprovalStatus approvalStatus, RestaurantStatus restaurantStatus, Set<ServiceType> enabledServices, Set<String> permissions, boolean onboardingComplete, Progress progress) {}
    public record Detail(UUID id, long version, UUID ownerId, String restaurantName, String description, RestaurantType restaurantType, String email, String phone, String alternatePhone, String addressLine1, String addressLine2, String city, String state, String postalCode, String country, BigDecimal latitude, BigDecimal longitude, String timezone, String businessRegistrationNumber, String gstNumber, String fssaiNumber, String logoUrl, String coverImageUrl, ApprovalStatus approvalStatus, RestaurantStatus status, Instant submittedAt, Instant approvedAt, UUID approvedBy, String rejectionReason, String changeRequestReason, Instant createdAt, Instant updatedAt, List<ServiceActivationView> services, List<OperatingHourView> operatingHours, List<SpecialScheduleView> specialSchedules, List<DocumentView> documents, ReservationSettingsView reservationSettings, TakeawaySettingsView takeawaySettings, DiniTicketSettingsView diniticketSettings, PolicyView policies, SettlementProfileView settlementProfile, TableSetupView tableSetup, MenuSetupView menuSetup, TeamAccessView teamAccess, List<TeamDraftView> teamMembers, Progress onboardingProgress, OnboardingForm onboardingForm) {}
}
