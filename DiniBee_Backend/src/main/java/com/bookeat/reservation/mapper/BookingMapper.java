package com.bookeat.reservation.mapper;
import com.bookeat.reservation.dto.BookingResponses.*;
import com.bookeat.reservation.entity.*;
import com.bookeat.reservation.repository.*;
import com.bookeat.customer.repository.CustomerRepository;
import com.bookeat.table.repository.RestaurantTableRepository;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.time.ZoneId;
import java.util.*;
@Component @RequiredArgsConstructor
public class BookingMapper {
    private final ReservationStatusHistoryRepository histories;
    private final CustomerRepository customers;
    private final RestaurantTableRepository tables;
    public Booking booking(Reservation r){var zone=ZoneId.of(r.getTimezone());var c=customers.findById(r.getCustomerId()).orElse(null); var u=c==null?null:c.getUser(); var name=u==null?"":((u.getFirstName()==null?"":u.getFirstName())+" "+(u.getLastName()==null?"":u.getLastName())).trim(); var t=tables.findByIdAndRestaurantIdAndDeletedAtIsNull(r.getTableId(),r.getRestaurantId()).orElse(null); var tn=t==null?null:t.getTableNumber(); var tname=t==null?null:t.getTableName(); return new Booking(r.getId(),r.getVersion(),publicCode(r.getBookingReference()),r.getCustomerId(),name,u==null?null:u.getEmail(),u==null?null:u.getPhoneNumber(),r.getRestaurantId(),r.getTableId(),tn,tname,r.getReservationDate(),r.getStartTime(),r.getEndTime(),r.getStartsAt().atZone(zone).toOffsetDateTime(),r.getEndsAt().atZone(zone).toOffsetDateTime(),r.getGuestCount(),r.getSpecialRequest(),r.getBookingType(),r.getStatus(),r.getPaymentStatus(),r.isCancellationAllowed(),r.getCancellationDeadline(),r.getCheckedInAt(),r.getCompletedAt(),r.getCancelledAt(),r.getCancellationReason(),histories.findByReservationIdOrderByChangedAtAscIdAsc(r.getId()).stream().map(h->new History(h.getOldStatus(),h.getNewStatus(),h.getReason(),h.getChangedAt())).toList());}
    private String publicCode(String value){if(value==null)return null;String clean=value.replaceAll("[^A-Za-z0-9]","").toUpperCase(Locale.ROOT);return clean.length()<=6?clean:clean.substring(0,6);}
    public Member member(GroupMember m,UUID host){return new Member(m.getId(),m.getDisplayName(),m.getMemberStatus(),m.getJoinedAt(),m.getCustomerId().equals(host));}
    public Account account(CorporateAccount a){return new Account(a.getId(),a.getCompanyName(),a.getGstNumber(),a.getBillingAddress(),a.getContactPersonName(),a.getContactEmail(),a.getContactPhone(),a.isActive());}
    public Guest guest(CorporateGuest g){return new Guest(g.getId(),g.getName(),g.getEmail(),g.getPhone(),g.getEmployeeCode(),g.getStatus());}
}
