package com.bookeat.diniticket.service;
import com.bookeat.diniticket.dto.*;
import com.bookeat.diniticket.entity.DiniTicket;
import com.bookeat.diniticket.enums.TicketStatus;
import com.bookeat.diniticket.repository.*;
import com.bookeat.restaurant.repository.DiniTicketSettingsRepository;
import com.bookeat.restaurant.entity.*;
import com.bookeat.restaurant.enums.ServiceType;
import com.bookeat.restaurant.service.RestaurantAccessService;
import com.bookeat.customer.repository.CustomerRepository;
import com.bookeat.availability.service.DiniTicketAllocationService;
import com.bookeat.availability.repository.TableAllocationRepository;
import com.bookeat.commerce.service.*;
import com.bookeat.common.response.PageResponse;
import com.bookeat.common.audit.AuditService;
import com.bookeat.exception.*;
import com.bookeat.order.repository.FoodOrderRepository;
import com.bookeat.order.service.OrderService;
import com.bookeat.order.enums.FoodOrderStatus;
import com.bookeat.payment.repository.PaymentRepository;
import com.bookeat.payment.service.PaymentTransactions;
import com.bookeat.payment.enums.*;
import com.bookeat.payment.dto.PaymentRequests;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional
public class DiniTicketService {
    private final DiniTicketRepository tickets;private final DiniTicketSettingsRepository settings;private final QueueEventRepository events;private final DiniTicketAllocationService seating;private final TableAllocationRepository allocations;
    private final CommerceAccess access;private final RestaurantAccessService owners;private final CustomerRepository customers;private final IdempotencyService idempotency;private final JdbcTemplate jdbc;private final AuditService audit;private final Clock clock;private final List<TicketCommitmentGuard> commitments;private final List<TicketSeatingObserver> seatingObservers;
    private final FoodOrderRepository foodOrders;private final OrderService orderService;private final PaymentRepository paymentRepository;private final PaymentTransactions paymentTransactions;private final DiniTicketSessionMenuService sessionMenu;private final DiniTicketSessionService diniSessions;
    public TicketResponse create(UUID actor,String key,TicketRequests.Create q){UUID id=idempotency.execute(actor,"diniticket",key,q,()->{var r=access.lock(q.restaurantId());var cfg=config(r);/* The current Restaurant Partner DiniTicket switch means customer pre-booking/live tokens are enabled. Legacy rows may still have allowRemoteJoin=false, so do not reject an otherwise enabled DiniTicket service here. */return issue(actor,r,access.customer(actor),q.guestCount(),cfg).getId();});return get(actor,id);}
    public TicketResponse walkIn(UUID actor,UUID rid,String key,UUID customerId,int guests){UUID id=idempotency.execute(actor,"walk-in-ticket",key,Map.of("restaurantId",rid,"customerId",customerId,"guests",guests),()->{var r=access.lockOwner(actor,rid);if(!customers.existsById(customerId))throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);return issue(actor,r,customerId,guests,config(r)).getId();});return ownerGet(actor,rid,id);}
    private DiniTicket issue(UUID actor,Restaurant r,UUID customer,int guests,DiniTicketSettings cfg){diniSessions.ensureActive(r.getId(),cfg.getMaximumQueueSize());expireLocked(r.getId(),cfg);/* DiniTicket is a pre-book/live-token service controlled by its own enabled switch. Do not reject a token merely because the restaurant's dine-in operating hours are currently closed; that made Partner show LIVE while Customer received SERVICE_NOT_AVAILABLE. config(r) above still enforces approved/active service access plus enabled DiniTicket settings. */if(tickets.existsByRestaurantIdAndCustomerIdAndStatusIn(r.getId(),customer,TicketRules.ACTIVE))throw new ApiException(ErrorCode.ACTIVE_TICKET_EXISTS);var queue=tickets.findByRestaurantIdAndStatusInOrderByQueueSequenceAsc(r.getId(),TicketRules.QUEUED);if(queue.size()>=cfg.getMaximumQueueSize())throw new ApiException(ErrorCode.QUEUE_FULL);
        long tokenNo=diniSessions.nextTokenNumber(r.getId(),cfg.getMaximumQueueSize());long seq=nextGlobalQueueSequence();var t=new DiniTicket();t.setTicketNumber(String.format(java.util.Locale.ROOT,"DT-%04d",tokenNo));t.setQueueSequence(seq);t.setRestaurantId(r.getId());t.setCustomerId(customer);t.setGuestCount(guests);t.setStatus(TicketStatus.WAITING);t.setQueuePosition(queue.size()+1);t.setEstimatedWaitMinutes(TicketRules.estimatedWait(queue.size()+1,cfg.getDefaultEstimatedMinutesPerParty()));t.setIssuedAt(clock.instant());t.setExpiresAt(clock.instant().plusSeconds(cfg.getTicketExpiryMinutes()*60L));tickets.saveAndFlush(t);events.append(t,"TICKET_CREATED");audit.record(actor,"TICKET_CREATED","DiniTicket",t.getId(),null);sessionMenu.archiveIfLimitReached(r.getId(),cfg.getMaximumQueueSize());return t;}
    private long nextGlobalQueueSequence(){
        Long next=jdbc.queryForObject("select nextval('dini_ticket_sequence')",Long.class);
        return next==null?1L:next;
    }
    public TicketResponse get(UUID actor,UUID id){UUID c=access.customer(actor);UUID rid=tickets.restaurantId(id).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));access.lock(rid);var t=tickets.findById(id).filter(x->x.getCustomerId().equals(c)).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));expireLocked(rid,settings.findByRestaurantId(rid).orElseThrow());return response(t);}
    public List<TicketResponse> current(UUID actor){UUID c=access.customer(actor);/* Current means current DiniTicket session only; historical active rows from an older session must never reappear after a new owner session starts. */return tickets.findByCustomerIdAndStatusInOrderByQueueSequenceAsc(c,TicketRules.ACTIVE).stream().filter(t->diniSessions.belongsToActive(t.getRestaurantId(),t.getIssuedAt())).map(this::response).toList();}
    @Transactional(readOnly=true) public PageResponse<TicketResponse> customerList(UUID actor,int page,int size){UUID c=access.customer(actor);return PageResponse.from(tickets.findByCustomerId(c,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"issuedAt","id"))).map(this::response));}
    public PageResponse<TicketResponse> ownerList(UUID actor,UUID rid,boolean waiting,int page,int size){
        // Read endpoints must not mutate queue state. Previously ownerList()
        // called expireLocked(), which could transition an expired ticket and
        // then fail with ORDER_IN_PROGRESS (HTTP 409) when that ticket had a
        // linked food-order/payment commitment. Restaurant Partner polls this
        // endpoint, so one historical commitment blanked Menu/Pre-orders/Queue/History.
        // Expiry remains handled by explicit lifecycle operations/scheduled flows.
        access.lockOwner(actor,rid);
        settings.findByRestaurantId(rid).orElseThrow(()->new ApiException(ErrorCode.SERVICE_NOT_AVAILABLE));
        if(!waiting)return PageResponse.from(tickets.findByRestaurantId(rid,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"queueSequence"))).map(this::response));
        var q=tickets.findByRestaurantIdAndStatusInOrderByQueueSequenceAsc(rid,TicketRules.QUEUED).stream().filter(t->diniSessions.belongsToActive(rid,t.getIssuedAt())).toList();
        int from=(int)Math.min((long)page*size,q.size()),to=Math.min(from+size,q.size());
        return new PageResponse<>(q.subList(from,to).stream().map(this::response).toList(),page,size,q.size(),(q.size()+size-1)/size,page==0,to>=q.size());
    }
    public DiniTicketLiveResponse live(UUID actor,UUID rid){
        var restaurant=access.lockOwner(actor,rid);
        var cfg=config(restaurant);
        // Read-only live queue: do not expire a token merely because the dashboard refreshes.
        var active=tickets.findByRestaurantIdAndStatusInOrderByQueueSequenceAsc(rid,TicketRules.ACTIVE).stream().filter(t->diniSessions.belongsToActive(rid,t.getIssuedAt())).toList();
        // A CALLED/ARRIVED/SEATED ticket is the real now-serving token.  If the
        // queue has not been called yet, expose the first WAITING token so a
        // newly generated customer token appears immediately in Restaurant Partner.
        var current=active.stream()
                .filter(t->t.getStatus()!=TicketStatus.WAITING)
                .findFirst()
                .or(()->active.stream().filter(t->t.getStatus()==TicketStatus.WAITING).findFirst());

        var next=active.stream()
                .filter(t->t.getStatus()==TicketStatus.WAITING)
                .filter(t->current.isEmpty()||!t.getId().equals(current.get().getId()))
                .limit(5)
                .map(this::response)
                .toList();
        long waiting=active.stream().filter(t->t.getStatus()==TicketStatus.WAITING).count();
        long called=active.stream().filter(t->t.getStatus()==TicketStatus.CALLED).count();
        long arrived=active.stream().filter(t->t.getStatus()==TicketStatus.ARRIVED).count();
        long seated=active.stream().filter(t->t.getStatus()==TicketStatus.SEATED).count();
        return new DiniTicketLiveResponse(current.map(this::response).orElse(null),next,waiting,called,arrived,seated);
    }
    public TicketResponse ownerGet(UUID actor,UUID rid,UUID id){
        // Keep GET read-only for the same reason as ownerList().
        access.lockOwner(actor,rid);
        settings.findByRestaurantId(rid).orElseThrow(()->new ApiException(ErrorCode.SERVICE_NOT_AVAILABLE));
        return response(tickets.findById(id).filter(t->t.getRestaurantId().equals(rid)).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND)));
    }
    public TicketResponse cancel(UUID actor,UUID id,TicketRequests.Action q){UUID rid=tickets.restaurantId(id).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));access.lock(rid);var t=tickets.findById(id).filter(x->x.getCustomerId().equals(access.customer(actor))).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));access.version(t.getVersion(),q.version());transition(t,actor,TicketStatus.CANCELLED,q.reason());return response(t);}
    public TicketResponse action(UUID actor,UUID rid,UUID id,TicketRequests.Action q,TicketStatus target){var r=access.lockOwner(actor,rid);var cfg=settings.findByRestaurantId(rid).orElseThrow();var t=tickets.findById(id).filter(x->x.getRestaurantId().equals(rid)).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));access.version(t.getVersion(),q.version());
        if(target==TicketStatus.CALLED||target==TicketStatus.ARRIVED){config(r);if(cfg.isAutoExpireTickets()&&!clock.instant().isBefore(t.getExpiresAt())&&mayExpire(t.getId()))throw new ApiException(ErrorCode.TICKET_STATE_INVALID);}
        if(target==TicketStatus.CALLED){var first=tickets.findByRestaurantIdAndStatusInOrderByQueueSequenceAsc(rid,Set.of(TicketStatus.WAITING)).stream().findFirst();if(first.isEmpty()||!first.get().getId().equals(id))throw new ApiException(ErrorCode.TICKET_STATE_INVALID);}
        // Owner/manager manual No Show is an explicit operational decision.
        // TicketRules still limits it to WAITING/CALLED, but do not force the
        // automatic expiry deadline before the button can be used.
        transition(t,actor,target,q.reason());return response(t);}
    public TicketResponse seat(UUID actor,UUID rid,UUID id,TicketRequests.Seat q){var r=access.lockOwner(actor,rid);config(r);var t=tickets.findById(id).filter(x->x.getRestaurantId().equals(rid)).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));access.version(t.getVersion(),q.version());if(t.getStatus()!=TicketStatus.ARRIVED)throw new ApiException(ErrorCode.TICKET_STATE_INVALID);if(q.tableId()!=null){t.setAllocationId(seating.claim(r,q.tableId(),id,t.getGuestCount()));t.setTableId(q.tableId());}transition(t,actor,TicketStatus.SEATED,q.reason());for(var observer:seatingObservers)observer.seated(t,actor);return response(t);}
    public TicketResponse handover(UUID actor,UUID rid,UUID id,TicketRequests.Handover q){
        var restaurant=access.lockOwner(actor,rid);config(restaurant);
        var t=tickets.findById(id).filter(x->x.getRestaurantId().equals(rid)).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        if(t.getStatus()==TicketStatus.COMPLETED)return response(t);
        access.version(t.getVersion(),q.version());
        String expected=t.getTicketNumber()==null?"":t.getTicketNumber().replaceAll("[^0-9]","");
        String supplied=q.tokenNumber()==null?"":q.tokenNumber().replaceAll("[^0-9]","");
        if(expected.isBlank()||supplied.isBlank())throw new ApiException(ErrorCode.VALIDATION_FAILED);
        try{
            if(Long.parseLong(expected)!=Long.parseLong(supplied))throw new ApiException(ErrorCode.VALIDATION_FAILED);
        }catch(NumberFormatException ex){throw new ApiException(ErrorCode.VALIDATION_FAILED);}

        // Pay-at-restaurant DiniTickets are confirmed only when the owner physically
        // hands over the order. This makes CASH idempotent and prevents a stale
        // PENDING payment from keeping the ticket open forever.
        for(var p:paymentRepository.findByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(PaymentReferenceType.DINITICKET,id)){
            if(p.getGateway()==PaymentGateway.CASH&&p.getStatus()==PaymentStatus.PENDING){
                paymentTransactions.cash(actor,rid,p.getId(),new PaymentRequests.Action(p.getVersion(),"Cash collected at DiniTicket handover"));
                break;
            }
        }

        if(t.getStatus()==TicketStatus.WAITING){
            var first=tickets.findByRestaurantIdAndStatusInOrderByQueueSequenceAsc(rid,Set.of(TicketStatus.WAITING)).stream().findFirst();
            if(first.isEmpty()||!first.get().getId().equals(id))throw new ApiException(ErrorCode.TICKET_STATE_INVALID);
            transition(t,actor,TicketStatus.CALLED,"Verified at DiniTicket handover");
        }
        if(t.getStatus()==TicketStatus.CALLED)transition(t,actor,TicketStatus.ARRIVED,"Customer arrived for DiniTicket handover");
        if(t.getStatus()==TicketStatus.ARRIVED){
            transition(t,actor,TicketStatus.SEATED,"DiniTicket handover without table allocation");
            for(var observer:seatingObservers)observer.seated(t,actor);
        }
        if(t.getStatus()!=TicketStatus.SEATED)throw new ApiException(ErrorCode.TICKET_STATE_INVALID);

        // Verification/handover is a distinct persisted stage.  Do NOT complete
        // the ticket here.  SEATED is the legacy database value used for the
        // DiniTicket HANDED_OVER stage and SERVED is the linked food-order
        // HANDED_OVER stage.  The customer tracker maps these values to the
        // explicit "Handed Over" step.  Final completion is a separate owner
        // action handled by completeHandover().
        for(var o:foodOrders.findByDiniTicketId(id)){
            if(o.getStatus()==FoodOrderStatus.CANCELLED||o.getStatus()==FoodOrderStatus.REJECTED||o.getStatus()==FoodOrderStatus.COMPLETED)continue;
            if(o.getStatus()==FoodOrderStatus.PLACED)orderService.transitionLocked(o,actor,FoodOrderStatus.ACCEPTED,"DiniTicket handover",false);
            if(o.getStatus()==FoodOrderStatus.ACCEPTED)orderService.transitionLocked(o,actor,FoodOrderStatus.PREPARING,"DiniTicket handover",false);
            if(o.getStatus()==FoodOrderStatus.PREPARING)orderService.transitionLocked(o,actor,FoodOrderStatus.READY,"DiniTicket handover",false);
            if(o.getStatus()==FoodOrderStatus.READY)orderService.transitionLocked(o,actor,FoodOrderStatus.SERVED,"DiniTicket handed over",false);
        }
        return response(t);
    }

    public TicketResponse completeHandover(UUID actor,UUID rid,UUID id,TicketRequests.Action q){
        access.lockOwner(actor,rid);
        var t=tickets.findById(id).filter(x->x.getRestaurantId().equals(rid)).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        if(t.getStatus()==TicketStatus.COMPLETED)return response(t);
        if(t.getStatus()==TicketStatus.CANCELLED||t.getStatus()==TicketStatus.NO_SHOW||t.getStatus()==TicketStatus.EXPIRED) {
            throw new ApiException(ErrorCode.TICKET_STATE_INVALID);
        }

        // Settle any pending cash payments for the ticket or linked orders at handover completion
        for(var p:paymentRepository.findByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(PaymentReferenceType.DINITICKET,id)){
            if(p.getGateway()==PaymentGateway.CASH&&p.getStatus()==PaymentStatus.PENDING){
                paymentTransactions.cash(actor,rid,p.getId(),new PaymentRequests.Action(p.getVersion(),"Cash collected at DiniTicket handover"));
                break;
            }
        }
        for(var food:foodOrders.findByDiniTicketId(id)){
            for(var p:paymentRepository.findByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(PaymentReferenceType.FOOD_ORDER,food.getId())){
                if(p.getGateway()==PaymentGateway.CASH&&p.getStatus()==PaymentStatus.PENDING){
                    paymentTransactions.cash(actor,rid,p.getId(),new PaymentRequests.Action(p.getVersion(),"Cash collected at DiniTicket handover"));
                    break;
                }
            }
        }

        if(t.getStatus()==TicketStatus.WAITING){
            transition(t,actor,TicketStatus.CALLED,"Auto-call before DiniTicket completion");
        }
        if(t.getStatus()==TicketStatus.CALLED){
            transition(t,actor,TicketStatus.ARRIVED,"Customer arrived for DiniTicket completion");
        }
        if(t.getStatus()==TicketStatus.ARRIVED){
            transition(t,actor,TicketStatus.SEATED,"DiniTicket handover before completion");
            for(var observer:seatingObservers)observer.seated(t,actor);
        }

        // Complete the exact linked preorder first so the ticket commitment
        // guard can safely allow the final ticket completion.
        for(var food:foodOrders.findByDiniTicketId(id)){
            if(food.getStatus()==FoodOrderStatus.CANCELLED||food.getStatus()==FoodOrderStatus.REJECTED||food.getStatus()==FoodOrderStatus.COMPLETED)continue;
            if(food.getStatus()==FoodOrderStatus.PLACED)orderService.transitionLocked(food,actor,FoodOrderStatus.ACCEPTED,"DiniTicket completion",false);
            if(food.getStatus()==FoodOrderStatus.ACCEPTED)orderService.transitionLocked(food,actor,FoodOrderStatus.PREPARING,"DiniTicket completion",false);
            if(food.getStatus()==FoodOrderStatus.PREPARING)orderService.transitionLocked(food,actor,FoodOrderStatus.READY,"DiniTicket completion",false);
            if(food.getStatus()==FoodOrderStatus.READY)orderService.transitionLocked(food,actor,FoodOrderStatus.SERVED,"DiniTicket completion",false);
            if(food.getStatus()==FoodOrderStatus.SERVED)orderService.transitionLocked(food,actor,FoodOrderStatus.COMPLETED,"DiniTicket completed",false);
        }
        transition(t,actor,TicketStatus.COMPLETED,q.reason()==null||q.reason().isBlank()?"DiniTicket completed after handover":q.reason());
        return response(t);
    }
    private void transition(DiniTicket t,UUID actor,TicketStatus target,String reason){if(t.getStatus()==target)return;if(!TicketRules.transition(t.getStatus(),target))throw new ApiException(ErrorCode.TICKET_STATE_INVALID);if(target==TicketStatus.CANCELLED||target==TicketStatus.NO_SHOW||target==TicketStatus.EXPIRED)for(var g:commitments)g.beforeCancel(t.getId(),actor);if(target==TicketStatus.COMPLETED){for(var g:commitments)g.beforeComplete(t.getId());seating.release(t.getRestaurantId(),t.getAllocationId(),t.getId());}
        t.setStatus(target);switch(target){case CALLED->{t.setCalledAt(clock.instant());var cfg=settings.findByRestaurantId(t.getRestaurantId()).orElseThrow();t.setExpiresAt(clock.instant().plusSeconds(cfg.getTicketExpiryMinutes()*60L));}case ARRIVED->t.setArrivedAt(clock.instant());case SEATED->t.setSeatedAt(clock.instant());case COMPLETED->t.setCompletedAt(clock.instant());default->{}}
        if(!TicketRules.QUEUED.contains(target)){t.setQueuePosition(0);t.setEstimatedWaitMinutes(0);}tickets.flush();events.append(t,"TICKET_"+target.name());audit.record(actor,"TICKET_"+target.name(),"DiniTicket",t.getId(),null);reposition(t.getRestaurantId());}
    public void expireRestaurant(UUID rid){access.lock(rid);settings.findByRestaurantId(rid).ifPresent(cfg->expireLocked(rid,cfg));}
    private void expireLocked(UUID rid,DiniTicketSettings cfg){if(!cfg.isAutoExpireTickets())return;for(var t:tickets.findByRestaurantIdAndStatusInOrderByQueueSequenceAsc(rid,Set.of(TicketStatus.WAITING,TicketStatus.CALLED)))if(!clock.instant().isBefore(t.getExpiresAt())&&mayExpire(t.getId()))transition(t,null,TicketStatus.EXPIRED,"Configured ticket expiry");}
    private void reposition(UUID rid){var cfg=settings.findByRestaurantId(rid).orElseThrow();int position=0;for(var t:tickets.findByRestaurantIdAndStatusInOrderByQueueSequenceAsc(rid,TicketRules.QUEUED)){position++;int wait=TicketRules.estimatedWait(position,cfg.getDefaultEstimatedMinutesPerParty());if(t.getQueuePosition()!=position||t.getEstimatedWaitMinutes()!=wait){t.setQueuePosition(position);t.setEstimatedWaitMinutes(wait);events.append(t,"QUEUE_POSITION_CHANGED");}}tickets.flush();}
    private DiniTicketSettings config(Restaurant r){access.requireService(r,ServiceType.DINITICKET);return settings.findByRestaurantId(r.getId()).filter(DiniTicketSettings::isEnabled).orElseThrow(()->new ApiException(ErrorCode.SERVICE_NOT_AVAILABLE));}
    private boolean mayExpire(UUID id){return commitments.stream().allMatch(g->g.mayExpire(id));}
    private TicketResponse response(DiniTicket t){Instant end=t.getAllocationId()==null?null:allocations.findById(t.getAllocationId()).map(a->a.getEndsAt()).orElse(null);boolean deferred=settings.findByRestaurantId(t.getRestaurantId()).map(DiniTicketSettings::isAutoExpireTickets).orElse(false)&&(t.getStatus()==TicketStatus.WAITING||t.getStatus()==TicketStatus.CALLED)&&!clock.instant().isBefore(t.getExpiresAt())&&!mayExpire(t.getId());return new TicketResponse(t.getId(),t.getVersion(),t.getTicketNumber(),t.getRestaurantId(),t.getGuestCount(),t.getQueuePosition(),t.getEstimatedWaitMinutes(),t.getStatus(),t.getIssuedAt(),t.getCalledAt(),t.getArrivedAt(),t.getSeatedAt(),t.getCompletedAt(),t.getExpiresAt(),t.getTableId(),end,deferred);}
}
