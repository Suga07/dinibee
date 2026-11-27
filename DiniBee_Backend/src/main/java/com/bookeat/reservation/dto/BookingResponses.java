package com.bookeat.reservation.dto;
import com.bookeat.reservation.enums.*;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
public final class BookingResponses {
    private BookingResponses() {}
    public record History(ReservationStatus oldStatus,ReservationStatus newStatus,String reason,Instant changedAt) {}
    public record Booking(UUID id,long version,String bookingReference,UUID customerId,String customerName,String customerEmail,String customerPhone,UUID restaurantId,UUID tableId,String tableNumber,String tableName,LocalDate reservationDate,LocalTime startTime,LocalTime endTime,OffsetDateTime startsAt,OffsetDateTime endsAt,int guestCount,String specialRequest,BookingType bookingType,ReservationStatus status,BookingPaymentStatus paymentStatus,boolean cancellationAllowed,Instant cancellationDeadline,Instant checkedInAt,Instant completedAt,Instant cancelledAt,String cancellationReason,List<History> history) {}
    public record Member(UUID id,String displayName,MemberStatus status,Instant joinedAt,boolean host) {}
    public record Group(UUID id,long version,Booking reservation,int memberLimit,GroupStatus status,GroupPaymentMode paymentMode,List<Member> members) {}
    public record Invitation(String inviteCode,String inviteUrl,String qrCode,Instant expiresAt) {}
    public record Account(UUID id,String companyName,String gstNumber,String billingAddress,String contactPersonName,String contactEmail,String contactPhone,boolean active) {}
    public record Guest(UUID id,String name,String email,String phone,String employeeCode,GuestStatus status) {}
    public record GuestInvite(Guest guest,Invitation invitation) {}
    public record Corporate(UUID id,long version,UUID seriesId,Account account,Booking reservation,BigDecimal budgetLimit,BillingType billingType,RecurrenceType recurrenceType,String recurrenceRule,boolean invoiceRequired,List<Guest> guests) {}
}
