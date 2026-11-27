package com.bookeat.reservation.service;
import com.bookeat.reservation.dto.*;
import com.bookeat.reservation.dto.BookingResponses.Booking;
import com.bookeat.reservation.entity.*;
import com.bookeat.reservation.enums.*;
import com.bookeat.reservation.repository.*;
import com.bookeat.reservation.mapper.BookingMapper;
import com.bookeat.commerce.service.*;
import com.bookeat.availability.service.TableAllocationService;
import com.bookeat.restaurant.repository.RestaurantReservationSettingsRepository;
import com.bookeat.restaurant.enums.ServiceType;
import com.bookeat.common.audit.AuditService;
import com.bookeat.common.response.PageResponse;
import com.bookeat.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.data.domain.*;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional
public class ReservationService {
    private final ReservationRepository reservations;private final ReservationStatusHistoryRepository history;
    private final GroupBookingRepository groups;private final GroupMemberRepository members;private final CorporateGuestRepository guests;private final CorporateBookingRepository corporate;
    private final CommerceAccess access;private final IdempotencyService idempotency;private final TableAllocationService allocations;private final RestaurantReservationSettingsRepository settings;
    private final BookingMapper mapper;private final AuditService audit;private final Clock clock;private final List<BookingCommitmentGuard> commitments;
    public Booking create(UUID actor,String key,BookingRequests.Create q){var id=idempotency.execute(actor,"reservation",key,q,()->createLocked(actor,q,BookingType.REGULAR).getId());return get(actor,id);}
    @Transactional(propagation=Propagation.MANDATORY) public Reservation createLocked(UUID actor,BookingRequests.Create q,BookingType type){
        var c=access.customer(actor);var restaurant=access.lock(q.restaurantId());access.requireService(restaurant,ServiceType.DINE_IN);
        var cfg=settings.findByRestaurantId(q.restaurantId()).orElseThrow(()->new ApiException(ErrorCode.SERVICE_NOT_AVAILABLE));
        if(q.specialRequest()!=null&&!q.specialRequest().isBlank()&&!cfg.isSpecialRequestAllowed())throw new ApiException(ErrorCode.VALIDATION_FAILED);
        var r=new Reservation();r.setBookingReference("BE"+UUID.randomUUID().toString().replace("-","").substring(0,4).toUpperCase(Locale.ROOT));r.setCustomerId(c);r.setRestaurantId(q.restaurantId());r.setTableId(q.tableId());r.setBookingType(type);r.setStatus(cfg.isAutoConfirm()?ReservationStatus.CONFIRMED:ReservationStatus.PENDING);r.setPaymentStatus(BookingPaymentStatus.UNPAID);r.setTimezone(restaurant.getTimezone());
        assign(r,q.startsAt(),q.guestCount(),q.specialRequest(),cfg);reservations.saveAndFlush(r);
        var claim=allocations.claimReservation(actor,r.getRestaurantId(),q.tableId(),r.getId(),q.startsAt(),q.guestCount());r.setAllocationId(claim.id());r.setEndsAt(claim.diningEndsAt());r.setEndTime(r.getEndsAt().atZone(ZoneId.of(r.getTimezone())).toLocalTime());
        changed(r,actor,null,"Reservation created");return r;
    }
    private void assign(Reservation r,OffsetDateTime start,int count,String special,com.bookeat.restaurant.entity.RestaurantReservationSettings cfg){var local=start.atZoneSameInstant(ZoneId.of(r.getTimezone()));r.setStartsAt(start.toInstant());r.setEndsAt(start.toInstant().plusSeconds(cfg.getReservationDurationMinutes()*60L));r.setReservationDate(local.toLocalDate());r.setStartTime(local.toLocalTime());r.setEndTime(r.getEndsAt().atZone(ZoneId.of(r.getTimezone())).toLocalTime());r.setGuestCount(count);r.setSpecialRequest(special);r.setCancellationAllowed(cfg.isCancellationAllowed());r.setCancellationDeadline(r.getStartsAt().minusSeconds(cfg.getCancellationBeforeMinutes()*60L));r.setNoShowAfter(r.getStartsAt().plusSeconds(cfg.getGracePeriodMinutes()*60L));}
    @Transactional(readOnly=true) public Booking get(UUID actor,UUID id){return mapper.booking(owned(actor,id));}
    @Transactional(readOnly=true) public PageResponse<Booking> list(UUID actor,int page,int size){return PageResponse.from(reservations.findByCustomerId(access.customer(actor),PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"startsAt","id"))).map(mapper::booking));}
    @Transactional(readOnly=true) public PageResponse<Booking> ownerList(UUID actor,UUID rid,int page,int size){ownerRead(actor,rid);return PageResponse.from(reservations.findByRestaurantId(rid,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"startsAt","id"))).map(mapper::booking));}
    @Transactional(readOnly=true) public Booking ownerGet(UUID actor,UUID rid,UUID id){ownerRead(actor,rid);var r=reservations.findById(id).filter(e->e.getRestaurantId().equals(rid)).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));return mapper.booking(r);}
    private final com.bookeat.restaurant.service.RestaurantAccessService restaurantOwners;
    private void ownerRead(UUID actor,UUID rid){restaurantOwners.getOwnedRestaurant(actor,rid);}
    public Booking change(UUID actor,UUID id,BookingRequests.Change q){var r=lockOwned(actor,id);if(r.getBookingType()==BookingType.CORPORATE)throw new ApiException(ErrorCode.BOOKING_STATE_INVALID);return mapper.booking(changeLocked(actor,r,q));}
    @Transactional(propagation=Propagation.MANDATORY) public Reservation changeLocked(UUID actor,Reservation r,BookingRequests.Change q){
        access.version(r.getVersion(),q.version());if(!BookingRules.editable(r.getStatus())||!clock.instant().isBefore(r.getStartsAt()))throw new ApiException(ErrorCode.BOOKING_STATE_INVALID);
        for(var g:commitments)g.beforeReschedule(r);
        var group=groups.findByReservationId(r.getId());if(group.isPresent()&&(group.get().getStatus()==GroupStatus.LOCKED||q.guestCount()<group.get().getMemberLimit()))throw new ApiException(ErrorCode.BOOKING_STATE_INVALID);
        var corp=corporate.findByReservationId(r.getId());if(corp.isPresent()&&q.guestCount()<guests.countByCorporateBookingIdAndStatusNot(corp.get().getId(),GuestStatus.REMOVED)+1)throw new ApiException(ErrorCode.VALIDATION_FAILED);
        var cfg=settings.findByRestaurantId(r.getRestaurantId()).orElseThrow();if(q.specialRequest()!=null&&!q.specialRequest().isBlank()&&!cfg.isSpecialRequestAllowed())throw new ApiException(ErrorCode.VALIDATION_FAILED);
        allocations.release(actor,r.getRestaurantId(),r.getAllocationId(),r.getId());var claim=allocations.claimReservation(actor,r.getRestaurantId(),q.tableId(),r.getId(),q.startsAt(),q.guestCount());r.setAllocationId(claim.id());r.setTableId(q.tableId());assign(r,q.startsAt(),q.guestCount(),q.specialRequest(),cfg);
        var old=r.getStatus();r.setStatus(cfg.isAutoConfirm()?ReservationStatus.CONFIRMED:ReservationStatus.PENDING);changed(r,actor,old,"Reservation rescheduled; availability revalidated");return r;
    }
    public Booking cancel(UUID actor,UUID id,BookingRequests.Action q){var r=lockOwned(actor,id);if(r.getStatus()==ReservationStatus.CANCELLED)return mapper.booking(r);access.version(r.getVersion(),q.version());if(!BookingRules.cancellable(r.getStatus(),r.isCancellationAllowed(),r.getCancellationDeadline(),clock.instant()))throw new ApiException(ErrorCode.CANCELLATION_NOT_ALLOWED);terminate(r,actor,ReservationStatus.CANCELLED,q.reason());return mapper.booking(r);}
    public Booking ownerChange(UUID actor,UUID rid,UUID id,BookingRequests.Change q){
        access.lockOwner(actor,rid);
        var r=reservations.findById(id).filter(e->e.getRestaurantId().equals(rid)).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        if(r.getBookingType()==BookingType.CORPORATE)throw new ApiException(ErrorCode.BOOKING_STATE_INVALID);
        return mapper.booking(changeLocked(actor,r,q));
    }
    public Booking ownerStartDining(UUID actor,UUID rid,UUID id,BookingRequests.Action q){
        access.lockOwner(actor,rid);
        var r=reservations.findById(id).filter(e->e.getRestaurantId().equals(rid)).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        access.version(r.getVersion(),q.version());
        if(r.getStatus()==ReservationStatus.DINING)return mapper.booking(r);
        if(r.getStatus()!=ReservationStatus.ARRIVED && r.getStatus()!=ReservationStatus.CHECKED_IN)throw new ApiException(ErrorCode.BOOKING_STATE_INVALID);
        var old=r.getStatus();
        r.setStatus(ReservationStatus.DINING);
        if(r.getCheckedInAt()==null)r.setCheckedInAt(clock.instant());
        changed(r,actor,old,q.reason()==null||q.reason().isBlank()?"Customer checked in by restaurant":q.reason());
        return mapper.booking(r);
    }
    public Booking ownerComplete(UUID actor,UUID rid,UUID id,BookingRequests.Action q){
        access.lockOwner(actor,rid);
        var r=reservations.findById(id).filter(e->e.getRestaurantId().equals(rid)).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        if(r.getStatus()==ReservationStatus.COMPLETED)return mapper.booking(r);
        access.version(r.getVersion(),q.version());
        if(r.getStatus()!=ReservationStatus.ARRIVED && r.getStatus()!=ReservationStatus.CHECKED_IN && r.getStatus()!=ReservationStatus.DINING)throw new ApiException(ErrorCode.BOOKING_STATE_INVALID);
        var old=r.getStatus();
        r.setStatus(ReservationStatus.COMPLETED);
        r.setCompletedAt(clock.instant());
        if(r.getCheckedInAt()==null)r.setCheckedInAt(clock.instant());
        allocations.release(actor,r.getRestaurantId(),r.getAllocationId(),r.getId());
        changed(r,actor,old,q.reason()==null||q.reason().isBlank()?"Dining completed by restaurant":q.reason());
        return mapper.booking(r);
    }
    public Booking ownerSetArrival(UUID actor,UUID rid,UUID id,boolean arrived){access.lockOwner(actor,rid);var r=reservations.findById(id).filter(e->e.getRestaurantId().equals(rid)).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));var old=r.getStatus();if(arrived){if(old!=ReservationStatus.ARRIVED){if(old!=ReservationStatus.PENDING&&old!=ReservationStatus.CONFIRMED)throw new ApiException(ErrorCode.BOOKING_STATE_INVALID);r.setStatus(ReservationStatus.ARRIVED);changed(r,actor,old,"Customer marked arrived by restaurant");}}else{if(old==ReservationStatus.ARRIVED){r.setStatus(ReservationStatus.CONFIRMED);changed(r,actor,old,"Customer arrival reset by restaurant");}}return mapper.booking(r);}
    public Booking ownerAction(UUID actor,UUID rid,UUID id,BookingRequests.Action q,ReservationStatus target){access.lockOwner(actor,rid);var r=reservations.findById(id).filter(e->e.getRestaurantId().equals(rid)).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));access.version(r.getVersion(),q.version());if(r.getStatus()==target)return mapper.booking(r);if(!BookingRules.ownerTransition(r.getStatus(),target))throw new ApiException(ErrorCode.BOOKING_STATE_INVALID);
        if(target==ReservationStatus.NO_SHOW&&clock.instant().isBefore(r.getNoShowAfter()))throw new ApiException(ErrorCode.BOOKING_STATE_INVALID);
        if(target==ReservationStatus.CONFIRMED){access.requireService(access.lock(rid),ServiceType.DINE_IN);if(!clock.instant().isBefore(r.getStartsAt()))throw new ApiException(ErrorCode.BOOKING_STATE_INVALID);var old=r.getStatus();r.setStatus(target);changed(r,actor,old,q.reason());}else terminate(r,actor,target,q.reason());return mapper.booking(r);}
    private void terminate(Reservation r,UUID actor,ReservationStatus target,String reason){for(var g:commitments)g.beforeCancel(r,actor);var old=r.getStatus();r.setStatus(target);r.setCancelledAt(clock.instant());r.setCancellationReason(reason);allocations.release(actor,r.getRestaurantId(),r.getAllocationId(),r.getId());groups.findByReservationId(r.getId()).ifPresent(g->{g.setStatus(GroupStatus.CANCELLED);g.setInviteHash(null);g.setInviteExpiresAt(null);});changed(r,actor,old,reason);}
    @Transactional(propagation=Propagation.MANDATORY) public Reservation lockOwned(UUID actor,UUID id){UUID c=access.customer(actor);var rid=reservations.restaurantId(id).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));access.lock(rid);return reservations.findByIdAndCustomerId(id,c).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));}
    @Transactional(readOnly=true) public Reservation owned(UUID actor,UUID id){return reservations.findByIdAndCustomerId(id,access.customer(actor)).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));}
    private void changed(Reservation r,UUID actor,ReservationStatus old,String reason){var h=new ReservationStatusHistory();h.setReservationId(r.getId());h.setOldStatus(old);h.setNewStatus(r.getStatus());h.setChangedBy(actor);h.setChangedAt(clock.instant());h.setReason(reason);history.save(h);audit.record(actor,"RESERVATION_"+r.getStatus().name(),"Reservation",r.getId(),null);reservations.flush();}
}
