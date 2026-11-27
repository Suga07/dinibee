package com.bookeat.restaurant.dto;
import com.bookeat.restaurant.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
public final class RestaurantRequests {
 private RestaurantRequests() {}
    public record Form(@NotNull @PositiveOrZero Long version,
        @NotNull @Valid OnboardingForm form) {}
    public record CreateRestaurant(
        @NotBlank @Size(max=160) String restaurantName,
        @NotNull RestaurantType restaurantType,
        @NotBlank @Size(max=320) @Email String email,
        @NotBlank @Size(max=20) String phone) {}
    public record Details(
        @NotNull @PositiveOrZero Long version,
        @NotBlank @Size(max=160) String restaurantName,
        @Size(max=3000) String description,
        @NotNull RestaurantType restaurantType,
        @NotBlank @Size(max=320) @Email String email,
        @NotBlank @Size(max=20) @Pattern(regexp="\\+[1-9][0-9]{7,14}") String phone,
        @Size(max=20) @Pattern(regexp="\\+[1-9][0-9]{7,14}") String alternatePhone,
        @Size(max=2048) @Pattern(regexp="https://[^\\s]+", message="must be an HTTPS URL") String logoUrl,
        @Size(max=2048) @Pattern(regexp="https://[^\\s]+", message="must be an HTTPS URL") String coverImageUrl) {}
    public record Location(
        @NotNull @PositiveOrZero Long version,
        @NotBlank @Size(max=250) String addressLine1,
        @Size(max=250) String addressLine2,
        @NotBlank @Size(max=100) String city,
        @NotBlank @Size(max=100) String state,
        @NotBlank @Size(max=20) String postalCode,
        @NotBlank @Size(max=2) @Pattern(regexp="[A-Z]{2}") String country,
        @NotNull @DecimalMin("-90") @DecimalMax("90") @Digits(integer=3,fraction=7) BigDecimal latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") @Digits(integer=3,fraction=7) BigDecimal longitude,
        @NotBlank @Size(max=60) String timezone) {}
    public record BusinessInfo(
        @NotNull @PositiveOrZero Long version,
        @NotBlank @Size(max=100) String businessRegistrationNumber,
        @Size(max=30) @Pattern(regexp="[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][A-Z0-9]Z[A-Z0-9]") String gstNumber,
        @NotBlank @Size(max=30) @Pattern(regexp="[0-9]{14}") String fssaiNumber) {}
    public record ReservationSettings(
        @NotNull @PositiveOrZero Long version,
        @NotNull Boolean reservationEnabled,
        @NotNull @Min(1) @Max(100) Integer minimumGuests,
        @NotNull @Min(1) @Max(500) Integer maximumGuestsPerBooking,
        @NotNull @Min(5) @Max(240) Integer slotDurationMinutes,
        @NotNull @Min(5) @Max(720) Integer reservationDurationMinutes,
        @NotNull @Min(1) @Max(365) Integer advanceBookingDays,
        @NotNull @Min(0) @Max(525600) Integer minimumAdvanceMinutes,
        @NotNull @Min(0) @Max(120) Integer gracePeriodMinutes,
        @NotNull Boolean cancellationAllowed,
        @NotNull @Min(0) @Max(525600) Integer cancellationBeforeMinutes,
        @NotNull Boolean autoConfirm,
        @NotNull Boolean specialRequestAllowed,
        @NotNull Boolean preOrderAllowed,
        @NotNull Boolean gpsCheckInEnabled,
        @NotNull Boolean qrCheckInEnabled,
        @NotNull @Min(10) @Max(1000) Integer gpsCheckInRadiusMeters) {}
    public record TakeawaySettings(
        @NotNull @PositiveOrZero Long version,
        @NotNull Boolean enabled,
        @NotNull Boolean regularTakeawayEnabled,
        @NotNull Boolean onTheWayEnabled,
        @NotNull @DecimalMin("0.00") @DecimalMax("9999999999.99") @Digits(integer=10,fraction=2) BigDecimal minimumOrderAmount,
        @NotNull @Min(1) @Max(1440) Integer averagePreparationMinutes,
        @NotNull @Min(1) @Max(10080) Integer maximumAdvanceOrderMinutes,
        @NotNull Boolean acceptCash,
        @NotNull Boolean acceptOnlinePayment) {}
    public record DiniSettings(
        @NotNull @PositiveOrZero Long version,
        @NotNull Boolean enabled,
        @NotNull @Min(1) @Max(1000) Integer maximumQueueSize,
        @NotNull @Min(1) @Max(240) Integer defaultEstimatedMinutesPerParty,
        @NotNull Boolean allowPreOrder,
        @NotNull Boolean allowRemoteJoin,
        @NotNull @Min(5) @Max(1440) Integer ticketExpiryMinutes,
        @NotNull Boolean autoExpireTickets) {}
    public record Policies(
        @NotNull @PositiveOrZero Long version,
        @NotBlank @Size(max=3000) String cancellationPolicy,
        @NotBlank @Size(max=3000) String refundPolicy,
        @Size(max=3000) String childPolicy,
        @Size(max=3000) String petPolicy,
        @Size(max=3000) String dressCode,
        @Size(max=3000) String additionalPolicy) {}
    public record TableSetup(
        @NotNull @PositiveOrZero Long version,
        @NotNull @Min(1) @Max(1000) Integer plannedTableCount,
        @NotNull @Min(1) @Max(30000) Integer plannedSeatCount,
        @Size(max=500) String sectionDescription) {}
    public record MenuSetup(
        @NotNull @PositiveOrZero Long version,
        @NotNull MenuSetupMode mode,
        @Size(max=2048) @Pattern(regexp="https://[^\\s]+", message="must be an HTTPS URL") String externalMenuUrl,
        @Size(max=1000) String notes,
        @NotNull Boolean acknowledged) {}
    public record SettlementProfile(
        @NotNull @PositiveOrZero Long version,
        @NotBlank @Size(max=160) String accountHolderName,
        @NotBlank @Size(max=160) String bankName,
        @NotBlank @Size(max=11) @Pattern(regexp="[A-Z]{4}0[A-Z0-9]{6}") String ifscCode,
        @Size(max=150) String upiId,
        @NotBlank @Size(max=160) String taxName,
        @Size(max=30) @Pattern(regexp="[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][A-Z0-9]Z[A-Z0-9]") String gstNumber,
        @NotBlank @Pattern(regexp="[0-9]{8,34}") @Schema(accessMode=Schema.AccessMode.WRITE_ONLY) String accountNumber) {}
    public record VersionRequest(@NotNull @PositiveOrZero Long version) {}
    public record Decision(@NotNull @PositiveOrZero Long version, @NotBlank @Size(max=2000) String reason) {}
    public record Approve(@NotNull @PositiveOrZero Long version, @NotEmpty Set<@NotNull ServiceType> services, @Size(max=2000) String reason) {}
    public record ServiceSelection(@NotNull @PositiveOrZero Long version, @NotEmpty Set<@NotNull ServiceType> services) {}
    public record ServiceDecision(@NotNull @PositiveOrZero Long version, @NotNull Boolean enabled, @NotBlank @Size(max=2000) String reason) {}
    public record Verification(@NotNull @PositiveOrZero Long version, @NotNull Boolean verified, @Size(max=2000) String reason) {}
    public record DocumentUpload(@NotNull @PositiveOrZero Long version, @NotNull DocumentType documentType, @NotBlank @Size(max=100) String documentNumber) {}
    public record Hour(@NotNull DayOfWeek dayOfWeek, LocalTime openingTime, LocalTime closingTime, @NotNull Boolean closed, @NotNull Boolean closesNextDay) {}
    public record Hours(@NotNull @PositiveOrZero Long version, @NotNull @Size(min=7,max=7) List<@NotNull @Valid Hour> hours) {}
    public record SpecialSchedule(@NotNull LocalDate date, @NotNull Boolean closed, LocalTime openingTime, LocalTime closingTime, @NotNull Boolean closesNextDay, @NotBlank @Size(max=500) String reason) {}
    public record SpecialSchedules(@NotNull @PositiveOrZero Long version, @NotNull @Size(max=365) List<@NotNull @Valid SpecialSchedule> schedules) {}
    public record TeamMember(@NotBlank @Email @Size(max=320) String email, @NotNull TeamRole role) {}
    public record TeamAccess(@NotNull @PositiveOrZero Long version, @NotNull TeamAccessMode mode, @NotNull Boolean acknowledged, @NotNull @Size(max=50) List<@NotNull @Valid TeamMember> members) {}
}
