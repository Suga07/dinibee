package com.bookeat.order.service;
import com.bookeat.order.dto.*;
import com.bookeat.order.entity.*;
import com.bookeat.order.enums.*;
import com.bookeat.order.repository.*;
import com.bookeat.order.mapper.OrderMapper;
import com.bookeat.restaurant.enums.ServiceType;
import com.bookeat.restaurant.repository.RestaurantTakeawaySettingsRepository;
import com.bookeat.restaurant.service.RestaurantAccessService;
import com.bookeat.commerce.service.*;
import com.bookeat.reservation.service.InviteTokens;
import com.bookeat.common.audit.AuditService;
import com.bookeat.common.response.PageResponse;
import com.bookeat.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.data.domain.*;
import java.time.*;
import java.util.*;
import java.math.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
@Service @RequiredArgsConstructor @Transactional
public class TakeawayService {
    private final TakeawayOrderRepository takeaways;private final FoodOrderRepository orders;private final OnTheWayOrderTrackingRepository tracking;private final OrderService food;private final OrderMapper mapper;
    private final CommerceAccess access;private final RestaurantAccessService owners;private final IdempotencyService idempotency;private final RestaurantTakeawaySettingsRepository settings;private final Clock clock;private final com.bookeat.payment.gateway.GatewayPort paymentGateway;private final AuditService audit;private final RestaurantWindowService windows;private final com.bookeat.payment.repository.PaymentRepository payments;private final com.bookeat.payment.service.PaymentTransactions paymentTransactions;
    public OrderResponses.Takeaway create(UUID actor,String key,OrderRequests.Takeaway q){UUID id=idempotency.execute(actor,"takeaway",key,q,()->{var r=access.lock(q.restaurantId());access.requireService(r,ServiceType.TAKEAWAY);var cfg=settings.findByRestaurantId(q.restaurantId()).orElseThrow(()->new ApiException(ErrorCode.SERVICE_NOT_AVAILABLE));
        if(!cfg.isEnabled()||(!cfg.isAcceptCash()&&(!cfg.isAcceptOnlinePayment()||!paymentGateway.enabled()))||(q.onTheWay()&&!cfg.isOnTheWayEnabled())||(!q.onTheWay()&&!cfg.isRegularTakeawayEnabled()))throw new ApiException(ErrorCode.SERVICE_NOT_AVAILABLE);
        Instant now=clock.instant(),pickup=q.pickupTime().toInstant();if(pickup.isBefore(now.plusSeconds(cfg.getAveragePreparationMinutes()*60L))||pickup.isAfter(now.plusSeconds(cfg.getMaximumAdvanceOrderMinutes()*60L))||!windows.contains(r,pickup.minusSeconds(cfg.getAveragePreparationMinutes()*60L),pickup.plusNanos(1)))throw new ApiException(ErrorCode.VALIDATION_FAILED);
        var o=food.createLocked(actor,new OrderRequests.Create(q.restaurantId(),q.onTheWay()?OrderType.ON_THE_WAY:OrderType.TAKEAWAY,null,null,q.items(),q.specialInstructions()),true);if(o.getTotal().compareTo(cfg.getMinimumOrderAmount())<0)throw new ApiException(ErrorCode.VALIDATION_FAILED);
        var t=new TakeawayOrder();t.setFoodOrderId(o.getId());t.setOrderNumber(nextShortOrderNumber());t.setCustomerId(o.getCustomerId());t.setRestaurantId(r.getId());t.setPickupTime(pickup);t.setStatus(TakeawayStatus.PLACED);t.setSubtotal(o.getSubtotal());t.setDiscount(o.getDiscount());t.setTax(o.getTax());t.setTotal(o.getTotal());t.setPaymentStatus(OrderPaymentStatus.UNPAID);t.setPickupCode(InviteTokens.issue().substring(0,12));takeaways.saveAndFlush(t);o.setTakeawayOrderId(t.getId());orders.flush();return t.getId();});return get(actor,id);}
    @Transactional(readOnly=true) public OrderResponses.Takeaway get(UUID actor,UUID id){return response(owned(actor,id),true);}
    @Transactional(readOnly=true) public PageResponse<OrderResponses.Takeaway> list(UUID actor,int page,int size){return PageResponse.from(takeaways.findByCustomerId(access.customer(actor),PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt","id"))).map(t->response(t,true)));}
    @Transactional(readOnly=true) public PageResponse<OrderResponses.Takeaway> ownerList(UUID actor,UUID rid,int page,int size){owners.getOwnedRestaurant(actor,rid);return PageResponse.from(takeaways.findConfirmedByRestaurantId(rid,PageRequest.of(page,size,Sort.by("pickupTime","id"))).map(t->response(t,false)));}
    @Transactional(readOnly=true) public OrderResponses.Takeaway ownerGet(UUID actor,UUID rid,UUID id){owners.getOwnedRestaurant(actor,rid);return response(takeaways.findById(id).filter(t->t.getRestaurantId().equals(rid)).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND)),false);}
    public OrderResponses.Takeaway cancel(UUID actor,UUID id,OrderRequests.Action q){access.lock(takeaways.restaurantId(id).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND)));var t=owned(actor,id);access.version(t.getVersion(),q.version());transition(t,actor,FoodOrderStatus.CANCELLED,q.reason());return response(t,true);}
    public OrderResponses.Takeaway ownerAction(UUID actor,UUID rid,UUID id,OrderRequests.Action q,FoodOrderStatus target){var r=access.lockOwner(actor,rid);var t=takeaways.findById(id).filter(x->x.getRestaurantId().equals(rid)).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));access.version(t.getVersion(),q.version());if(target==FoodOrderStatus.ACCEPTED)access.requireService(r,ServiceType.TAKEAWAY);transition(t,actor,target,q.reason());return response(t,false);}
    public OrderResponses.Takeaway collect(UUID actor,UUID rid,UUID id,OrderRequests.Collect q){access.lockOwner(actor,rid);var t=takeaways.findById(id).filter(x->x.getRestaurantId().equals(rid)).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));access.version(t.getVersion(),q.version());if(!MessageDigest.isEqual(t.getPickupCode().getBytes(StandardCharsets.UTF_8),q.pickupCode().getBytes(StandardCharsets.UTF_8)))throw new ApiException(ErrorCode.VALIDATION_FAILED);transition(t,actor,FoodOrderStatus.COMPLETED,"Pickup code verified; collected");return response(t,false);}
    public OrderResponses.Takeaway collectByOrderId(UUID actor,UUID rid,UUID id,OrderRequests.CollectByOrderId q){
        access.lockOwner(actor,rid);
        var t=takeaways.findById(id).filter(x->x.getRestaurantId().equals(rid)).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        if(q.orderNumber()==null||!t.getOrderNumber().equalsIgnoreCase(q.orderNumber().trim())) throw new ApiException(ErrorCode.VALIDATION_FAILED);

        // v46: public order-id handover is fully idempotent. A browser double-click,
        // retry, refresh, or a stale owner screen must never turn a successful handover
        // into HTTP 409.
        var o=orders.findById(t.getFoodOrderId()).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        if(t.getStatus()==TakeawayStatus.COLLECTED || o.getStatus()==FoodOrderStatus.COMPLETED){
            t.setStatus(TakeawayStatus.COLLECTED);
            if(t.getCollectedAt()==null) t.setCollectedAt(clock.instant());
            erase(t.getId());
            takeaways.flush();
            return response(t,false);
        }

        if(t.getStatus()==TakeawayStatus.CANCELLED || t.getStatus()==TakeawayStatus.REJECTED) {
            throw new ApiException(ErrorCode.ORDER_STATE_INVALID);
        }

        // Pay-at-restaurant/CASH is settled by the owner at physical handover.
        // Re-read/settle a pending cash intent before OrderService runs the payment guard.
        var pendingCash=payments.findFirstByCustomerIdAndReferenceTypeAndReferenceIdAndStatusInOrderByCreatedAtDesc(
            t.getCustomerId(),com.bookeat.payment.enums.PaymentReferenceType.TAKEAWAY,t.getId(),
            java.util.List.of(com.bookeat.payment.enums.PaymentStatus.CREATED,com.bookeat.payment.enums.PaymentStatus.PENDING));
        if (pendingCash.isEmpty()) {
            pendingCash = payments.findFirstByCustomerIdAndReferenceTypeAndReferenceIdAndStatusInOrderByCreatedAtDesc(
                t.getCustomerId(), com.bookeat.payment.enums.PaymentReferenceType.FOOD_ORDER, o.getId(),
                java.util.List.of(com.bookeat.payment.enums.PaymentStatus.CREATED, com.bookeat.payment.enums.PaymentStatus.PENDING));
        }
        if(pendingCash.isPresent()&&pendingCash.get().getGateway()==com.bookeat.payment.enums.PaymentGateway.CASH){
            var cp=pendingCash.get();
            if(cp.getStatus()!=com.bookeat.payment.enums.PaymentStatus.SUCCESS){
                paymentTransactions.cash(actor,rid,cp.getId(),new com.bookeat.payment.dto.PaymentRequests.Action(cp.getVersion(),"Cash received at takeaway handover"));
            }
        } else if (!paymentTransactions.paid(o.getId(), o.getTotal())) {
            // Settle counter cash payment if order not yet marked paid
            try {
                UUID pid = paymentTransactions.reserve(t.getCustomerId(), "cash-handover-" + t.getId(),
                    new com.bookeat.payment.dto.PaymentRequests.Create(
                        com.bookeat.payment.enums.PaymentReferenceType.TAKEAWAY, t.getId(),
                        com.bookeat.payment.enums.PaymentGateway.CASH, null
                    )
                );
                var p = payments.findById(pid).orElse(null);
                if (p != null) {
                    paymentTransactions.cash(actor, rid, p.getId(), new com.bookeat.payment.dto.PaymentRequests.Action(p.getVersion(), "Cash collected at takeaway handover"));
                }
            } catch (Exception ex) {
                // Ignore if already settled or conflict
            }
        }

        // Older records can have the takeaway READY while the linked food order is one
        // step behind. Normalize the linked order first, then complete exactly once.
        if(o.getStatus()==FoodOrderStatus.PLACED) food.transitionLocked(o,actor,FoodOrderStatus.ACCEPTED,"Takeaway state repair before handover",true);
        if(o.getStatus()==FoodOrderStatus.ACCEPTED) food.transitionLocked(o,actor,FoodOrderStatus.PREPARING,"Takeaway state repair before handover",true);
        if(o.getStatus()==FoodOrderStatus.PREPARING) food.transitionLocked(o,actor,FoodOrderStatus.READY,"Takeaway state repair before handover",true);
        if(o.getStatus()!=FoodOrderStatus.READY && o.getStatus()!=FoodOrderStatus.COMPLETED) throw new ApiException(ErrorCode.ORDER_STATE_INVALID);

        if(o.getStatus()!=FoodOrderStatus.COMPLETED){
            food.transitionLocked(o,actor,FoodOrderStatus.COMPLETED,"Public order ID verified; handed over and collected",true);
        }
        t.setStatus(TakeawayStatus.COLLECTED);
        t.setCollectedAt(clock.instant());
        erase(t.getId());
        takeaways.flush();
        return response(t,false);
    }
    private void transition(TakeawayOrder t,UUID actor,FoodOrderStatus target,String reason){var o=orders.findById(t.getFoodOrderId()).orElseThrow();food.transitionLocked(o,actor,target,reason,true);t.setStatus(switch(target){case ACCEPTED->TakeawayStatus.ACCEPTED;case PREPARING->TakeawayStatus.PREPARING;case READY->TakeawayStatus.READY_FOR_PICKUP;case COMPLETED->TakeawayStatus.COLLECTED;case CANCELLED->TakeawayStatus.CANCELLED;case REJECTED->TakeawayStatus.REJECTED;default->throw new ApiException(ErrorCode.ORDER_STATE_INVALID);});if(target==FoodOrderStatus.COMPLETED)t.setCollectedAt(clock.instant());if(OrderRules.terminal(target))erase(t.getId());takeaways.flush();}
    public OrderResponses.Tracking location(UUID actor,UUID id,OrderRequests.Location q){var r=access.lock(takeaways.restaurantId(id).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND)));var t=owned(actor,id);var o=orders.findById(t.getFoodOrderId()).orElseThrow();if(!Boolean.TRUE.equals(q.consent()))throw new ApiException(ErrorCode.LOCATION_CONSENT_REQUIRED);if(o.getOrderType()!=OrderType.ON_THE_WAY||OrderRules.terminal(o.getStatus())||r.getLatitude()==null||r.getLongitude()==null)throw new ApiException(ErrorCode.ORDER_STATE_INVALID);
        var v=tracking.findByTakeawayOrderId(id).orElseGet(OnTheWayOrderTracking::new);v.setTakeawayOrderId(id);v.setConsent(true);if(v.getConsentedAt()==null)v.setConsentedAt(clock.instant());v.setCustomerLatitude(q.latitude());v.setCustomerLongitude(q.longitude());v.setRestaurantLatitude(r.getLatitude());v.setRestaurantLongitude(r.getLongitude());double km=GeoRules.distanceKm(q.latitude().doubleValue(),q.longitude().doubleValue(),r.getLatitude().doubleValue(),r.getLongitude().doubleValue());v.setDistanceKm(BigDecimal.valueOf(km).setScale(3,RoundingMode.HALF_UP));v.setEstimatedArrival(clock.instant().plusSeconds(GeoRules.etaMinutes(km)*60));v.setLastLocationUpdatedAt(clock.instant());tracking.saveAndFlush(v);return track(v);}
    public void stopLocation(UUID actor,UUID id){access.lock(takeaways.restaurantId(id).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND)));owned(actor,id);erase(id);audit.record(actor,"LOCATION_CONSENT_REVOKED","TakeawayOrder",id,null);}
    private void erase(UUID id){tracking.findByTakeawayOrderId(id).ifPresent(v->{v.setConsent(false);v.setCustomerLatitude(null);v.setCustomerLongitude(null);v.setRestaurantLatitude(null);v.setRestaurantLongitude(null);v.setEstimatedArrival(null);v.setDistanceKm(null);v.setLastLocationUpdatedAt(null);v.setConsentedAt(null);});}

    private String nextShortOrderNumber(){
        // Customer-facing takeaway IDs are intentionally short: 4 uppercase
        // letters/numbers. Keep generating until the DB confirms uniqueness.
        final String alphabet="ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        for(int attempt=0;attempt<100;attempt++){
            String raw=UUID.randomUUID().toString().replace("-","").toUpperCase(Locale.ROOT);
            StringBuilder code=new StringBuilder(4);
            for(int i=0;i<4;i++){
                int n=Character.digit(raw.charAt(i),16);
                code.append(alphabet.charAt((n + raw.charAt(raw.length()-1-i)) % alphabet.length()));
            }
            String value=code.toString();
            if(!takeaways.existsByOrderNumber(value)) return value;
        }
        throw new ApiException(ErrorCode.CONFLICT);
    }
    private TakeawayOrder owned(UUID actor,UUID id){return takeaways.findById(id).filter(t->t.getCustomerId().equals(access.customer(actor))).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));}
    private OrderResponses.Takeaway response(TakeawayOrder t,boolean customer){return new OrderResponses.Takeaway(t.getId(),t.getVersion(),t.getOrderNumber(),t.getPickupTime(),t.getStatus(),t.getPaymentStatus(),customer?t.getPickupCode():null,t.getCollectedAt(),mapper.order(orders.findById(t.getFoodOrderId()).orElseThrow()),tracking.findByTakeawayOrderId(t.getId()).map(this::track).orElse(null));}
    private OrderResponses.Tracking track(OnTheWayOrderTracking t){return new OrderResponses.Tracking(t.isConsent(),t.getCustomerLatitude(),t.getCustomerLongitude(),t.getRestaurantLatitude(),t.getRestaurantLongitude(),t.getEstimatedArrival(),t.getDistanceKm(),t.getLastLocationUpdatedAt(),"STRAIGHT_LINE_AT_30_KMH; not a route or traffic estimate");}
}
