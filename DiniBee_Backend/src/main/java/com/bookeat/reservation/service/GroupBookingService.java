package com.bookeat.reservation.service;
import com.bookeat.reservation.dto.*;
import com.bookeat.reservation.entity.*;
import com.bookeat.reservation.enums.*;
import com.bookeat.reservation.repository.*;
import com.bookeat.reservation.mapper.BookingMapper;
import com.bookeat.commerce.service.*;
import com.bookeat.common.audit.AuditService;
import com.bookeat.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional
public class GroupBookingService {
    private final GroupBookingRepository groups;private final GroupMemberRepository members;private final ReservationRepository reservations;private final ReservationService booking;
    private final CommerceAccess access;private final IdempotencyService idempotency;private final BookingMapper mapper;private final AuditService audit;private final Clock clock;private final List<BookingCommitmentGuard> commitments;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    public BookingResponses.Group create(UUID actor,String key,BookingRequests.Group q){var id=idempotency.execute(actor,"group-booking",key,q,()->{access.require(q.memberLimit()<=q.reservation().guestCount());var r=booking.createLocked(actor,q.reservation(),BookingType.GROUP);var g=new GroupBooking();g.setReservationId(r.getId());g.setRestaurantId(r.getRestaurantId());g.setHostCustomerId(r.getCustomerId());g.setMemberLimit(q.memberLimit());g.setPaymentMode(q.paymentMode());g.setStatus(GroupStatus.ACTIVE);groups.saveAndFlush(g);joinMember(g,r.getCustomerId(),q.displayName());return g.getId();});return get(actor,id);}
    @Transactional(readOnly=true) public BookingResponses.Group get(UUID actor,UUID id){return response(readMember(actor,id));}
    public BookingResponses.Invitation invite(UUID actor,UUID id,long version){var g=lock(actor,id,true);access.version(g.getVersion(),version);active(g);String raw=InviteTokens.issue();g.setInviteHash(InviteTokens.hash(raw));var start=reservations.findById(g.getReservationId()).orElseThrow().getStartsAt();g.setInviteExpiresAt(clock.instant().plus(Duration.ofDays(2)).isBefore(start)?clock.instant().plus(Duration.ofDays(2)):start);groups.flush();audit.record(actor,"GROUP_INVITE_ROTATED","GroupBooking",id,null);String uri="bookeat://group/join/"+raw;return new BookingResponses.Invitation(raw,uri,uri,g.getInviteExpiresAt());}
    public BookingResponses.Group join(UUID actor,String invite,BookingRequests.Join q){String hash=InviteTokens.hash(invite);UUID rid=groups.restaurantIdByInvite(hash).orElseThrow(()->new ApiException(ErrorCode.INVITE_INVALID));access.lock(rid);var g=groups.findByInviteHash(hash).orElseThrow(()->new ApiException(ErrorCode.INVITE_INVALID));active(g);if(g.getInviteExpiresAt()==null||!clock.instant().isBefore(g.getInviteExpiresAt()))throw new ApiException(ErrorCode.INVITE_INVALID);UUID c=access.customer(actor);var old=members.findByGroupBookingIdAndCustomerId(g.getId(),c);if(old.isPresent()&&old.get().getMemberStatus()==MemberStatus.JOINED)return response(g);if(old.isPresent()&&old.get().getMemberStatus()==MemberStatus.REMOVED)throw new ApiException(ErrorCode.INVITE_INVALID);if(members.countByGroupBookingIdAndMemberStatus(g.getId(),MemberStatus.JOINED)>=g.getMemberLimit())throw new ApiException(ErrorCode.GROUP_FULL);joinMember(g,c,q.displayName());audit.record(actor,"GROUP_MEMBER_JOINED","GroupBooking",g.getId(),null);return response(g);}
    public BookingResponses.Group remove(UUID actor,UUID id,UUID memberId,long version){var g=lock(actor,id,true);access.version(g.getVersion(),version);active(g);var m=members.findById(memberId).filter(x->x.getGroupBookingId().equals(id)).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));if(m.getCustomerId().equals(g.getHostCustomerId()))throw new ApiException(ErrorCode.VALIDATION_FAILED);for(var guard:commitments)guard.beforeMemberRemoval(id,m.getCustomerId());m.setMemberStatus(MemberStatus.REMOVED);touch(g);audit.record(actor,"GROUP_MEMBER_REMOVED","GroupBooking",id,null);return response(g);}
    public BookingResponses.Group lockGroup(UUID actor,UUID id,long version){var g=lock(actor,id,true);access.version(g.getVersion(),version);active(g);g.setStatus(GroupStatus.LOCKED);g.setInviteHash(null);g.setInviteExpiresAt(null);groups.flush();audit.record(actor,"GROUP_LOCKED","GroupBooking",id,null);return response(g);}
    public Map<String, Object> remindGuests(UUID actor, UUID id) {
        var g = readMember(actor, id);
        var r = reservations.findById(g.getReservationId()).orElseThrow();
        String restName = jdbc.queryForObject("SELECT restaurant_name FROM restaurants WHERE id=?", String.class, g.getRestaurantId());
        String hostName = jdbc.query("SELECT display_name FROM group_members WHERE group_booking_id=? AND customer_id=?", (rs, n) -> rs.getString(1), id, g.getHostCustomerId()).stream().findFirst().orElse("Host");
        List<UUID> memberUsers = jdbc.query(
            "SELECT c.user_id FROM group_members gm JOIN customers c ON c.id=gm.customer_id WHERE gm.group_booking_id=? AND gm.member_status='JOINED'",
            (rs, n) -> rs.getObject(1, UUID.class), id
        );
        int sent = 0;
        long timeBucket = clock.instant().getEpochSecond() / 300;
        for (UUID u : memberUsers) {
            String dedup = "REMINDER_MANUAL:" + id + ":" + u + ":" + timeBucket;
            String title = "Group Booking Reminder";
            String msg = hostName + " has sent a reminder for your upcoming group booking at " + restName + " (" + r.getStartTime() + "). See you there!";
            int inserted = jdbc.update(
                "INSERT INTO notifications(id, user_id, title, message, notification_type, reference_type, reference_id, read_flag, deduplication_key) " +
                "VALUES(?,?,?,?,?,'Reservation',?,false,?) ON CONFLICT(user_id, deduplication_key) DO NOTHING",
                UUID.randomUUID(), u, title, msg, "GROUP_BOOKING_REMINDER", r.getId(), dedup
            );
            if (inserted > 0) sent++;
        }
        return Map.of("groupBookingId", id, "recipientsCount", memberUsers.size(), "sentCount", sent);
    }
    @Transactional(readOnly=true) public GroupBooking readMember(UUID actor,UUID id){var g=groups.findById(id).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));UUID c=access.customer(actor);if(!g.getHostCustomerId().equals(c)&&members.findByGroupBookingIdAndCustomerId(id,c).filter(m->m.getMemberStatus()==MemberStatus.JOINED).isEmpty())throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);return g;}
    private GroupBooking lock(UUID actor,UUID id,boolean host){access.lock(groups.restaurantId(id).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND)));var g=readMember(actor,id);if(host&&!g.getHostCustomerId().equals(access.customer(actor)))throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);return g;}
    private void active(GroupBooking g){var r=reservations.findById(g.getReservationId()).orElseThrow();if(g.getStatus()!=GroupStatus.ACTIVE||!BookingRules.editable(r.getStatus())||!clock.instant().isBefore(r.getStartsAt()))throw new ApiException(ErrorCode.BOOKING_STATE_INVALID);}
    private void joinMember(GroupBooking g,UUID c,String name){var m=members.findByGroupBookingIdAndCustomerId(g.getId(),c).orElseGet(GroupMember::new);m.setGroupBookingId(g.getId());m.setCustomerId(c);m.setDisplayName(name.trim());m.setMemberStatus(MemberStatus.JOINED);m.setJoinedAt(clock.instant());members.save(m);touch(g);}
    private void touch(GroupBooking g){g.setChangeSequence(g.getChangeSequence()+1);groups.flush();}
    private BookingResponses.Group response(GroupBooking g){return new BookingResponses.Group(g.getId(),g.getVersion(),mapper.booking(reservations.findById(g.getReservationId()).orElseThrow()),g.getMemberLimit(),g.getStatus(),g.getPaymentMode(),members.findByGroupBookingIdOrderByJoinedAtAscIdAsc(g.getId()).stream().map(m->mapper.member(m,g.getHostCustomerId())).toList());}
}
