package com.bookeat.reservation.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.bookeat.reservation.enums.*;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
public final class BookingRequests {
    private BookingRequests() {}
    public record Create(@NotNull UUID restaurantId,@NotNull UUID tableId,@NotNull OffsetDateTime startsAt,@Min(1) @Max(500) int guestCount,@Size(max=2000) String specialRequest) {}
    public record Change(@NotNull @PositiveOrZero Long version,@NotNull UUID tableId,@NotNull OffsetDateTime startsAt,@Min(1) @Max(500) int guestCount,@Size(max=2000) String specialRequest) {}
    public record Action(@NotNull @PositiveOrZero Long version,@NotBlank @Size(max=1000) String reason) {}
    public record Group(@NotNull @Valid Create reservation,@NotBlank @Size(max=100) String displayName,@Min(1) @Max(500) int memberLimit,@NotNull GroupPaymentMode paymentMode) {}
    public record Join(@NotBlank @Size(max=100) String displayName) {}
    public record Account(@NotBlank @Size(max=160) String companyName,@Size(max=30) String gstNumber,@NotBlank @Size(max=1000) String billingAddress,@NotBlank @Size(max=100) String contactPersonName,@NotBlank @Email @Size(max=320) String contactEmail,@NotBlank @Size(max=30) String contactPhone) {}
    public record Corporate(@NotNull @Valid Create reservation,UUID corporateAccountId,@Valid Account account,@NotNull @DecimalMin("0") @DecimalMax("9999999999999999.99") @Digits(integer=16,fraction=2) BigDecimal budgetLimit,@NotNull BillingType billingType,@NotNull RecurrenceType recurrenceType,@Min(1) @Max(12) int occurrences,@Min(1) @Max(12) int interval,@NotNull Boolean invoiceRequired) {}
    public record CorporateChange(@NotNull @Valid Change reservation,@NotNull @DecimalMin("0") @Digits(integer=16,fraction=2) BigDecimal budgetLimit,@NotNull BillingType billingType,@NotNull Boolean invoiceRequired) {}
    public record Guest(@NotBlank @Size(max=100) String name,@NotBlank @Email @Size(max=320) String email,@Size(max=30) String phone,@Size(max=80) String employeeCode) {}
}
