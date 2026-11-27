package com.bookeat.order.service;
import com.bookeat.order.dto.*;
import com.bookeat.order.entity.*;
import com.bookeat.order.enums.*;
import com.bookeat.order.repository.*;
import com.bookeat.order.mapper.OrderMapper;
import com.bookeat.menu.repository.*;
import com.bookeat.menu.service.MenuPricing;
import com.bookeat.reservation.repository.*;
import com.bookeat.reservation.enums.*;
import com.bookeat.reservation.entity.*;
import com.bookeat.reservation.service.*;
import com.bookeat.restaurant.enums.ServiceType;
import com.bookeat.restaurant.repository.RestaurantReservationSettingsRepository;
import com.bookeat.restaurant.service.RestaurantAccessService;
import com.bookeat.commerce.service.*;
import com.bookeat.common.audit.AuditService;
import com.bookeat.common.response.PageResponse;
import com.bookeat.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.data.domain.*;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
@Service @RequiredArgsConstructor @Transactional
public class OrderService {
    private final FoodOrderRepository orders;private final FoodOrderItemRepository lines;private final FoodOrderItemAddonRepository lineAddons;private final OrderStatusHistoryRepository history;
    private final MenuItemRepository menu;private final MenuVariantRepository variants;private final MenuAddonRepository addons;
    private final ReservationRepository reservations;private final GroupBookingRepository groups;private final GroupMemberRepository members;private final CorporateBookingRepository corporate;private final CorporateGuestRepository guests;
    private final RestaurantReservationSettingsRepository settings;private final CommerceAccess access;private final RestaurantAccessService owners;private final IdempotencyService idempotency;private final OrderMapper mapper;private final AuditService audit;private final Clock clock;private final List<TicketOrderAuthorizer> ticketAuthorizers;private final RestaurantOrderSettingsRepository orderSettings;private final List<OrderLifecycleGuard> lifecycleGuards;
    public OrderResponses.Order create(UUID actor,String key,OrderRequests.Create q){if(q.orderType()==OrderType.TAKEAWAY||q.orderType()==OrderType.ON_THE_WAY)throw new ApiException(ErrorCode.VALIDATION_FAILED);UUID id=idempotency.execute(actor,"food-order",key,q,()->createLocked(actor,q,false).getId());return get(actor,id);}
    @Transactional(propagation=Propagation.MANDATORY) public FoodOrder createLocked(UUID actor,OrderRequests.Create q,boolean takeaway){
        UUID c=access.customer(actor);var restaurant=access.lock(q.restaurantId());ServiceType service=takeaway?ServiceType.TAKEAWAY:q.orderType()==OrderType.DINITICKET_PREORDER?ServiceType.DINITICKET:ServiceType.DINE_IN;access.requireService(restaurant,service);
        var o=new FoodOrder();o.setOrderNumber(nextShortOrderNumber());o.setCustomerId(c);o.setRestaurantId(q.restaurantId());o.setOrderType(q.orderType());o.setStatus(FoodOrderStatus.PLACED);o.setSpecialInstructions(q.specialInstructions());o.setCurrency("INR");
        if(takeaway){access.require(q.reservationId()==null&&q.diniTicketId()==null&&(q.orderType()==OrderType.TAKEAWAY||q.orderType()==OrderType.ON_THE_WAY));}
        else if(q.orderType()==OrderType.DINITICKET_PREORDER){access.require(q.reservationId()==null&&q.diniTicketId()!=null);if(ticketAuthorizers.isEmpty())throw new ApiException(ErrorCode.SERVICE_NOT_AVAILABLE);for(var a:ticketAuthorizers)a.authorize(c,q.restaurantId(),q.diniTicketId());o.setDiniTicketId(q.diniTicketId());}
        else {access.require(q.reservationId()!=null&&q.diniTicketId()==null);var r=reservations.findById(q.reservationId()).filter(x->x.getRestaurantId().equals(q.restaurantId())).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
            if(q.orderType()==OrderType.DINE_IN_PREORDER){var cfg=settings.findByRestaurantId(r.getRestaurantId()).orElseThrow();if(!cfg.isPreOrderAllowed()||!BookingRules.editable(r.getStatus())||!clock.instant().isBefore(r.getStartsAt()))throw new ApiException(ErrorCode.BOOKING_STATE_INVALID);}else if(q.orderType()!=OrderType.DINE_IN||(r.getStatus()!=ReservationStatus.ARRIVED&&r.getStatus()!=ReservationStatus.CHECKED_IN&&r.getStatus()!=ReservationStatus.DINING))throw new ApiException(ErrorCode.BOOKING_STATE_INVALID);
            o.setReservationId(r.getId());
            switch(r.getBookingType()) {
                case REGULAR->{if(!r.getCustomerId().equals(c))throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);}
                case GROUP->{var g=groups.findByReservationId(r.getId()).orElseThrow();if(g.getStatus()!=GroupStatus.ACTIVE||members.findByGroupBookingIdAndCustomerId(g.getId(),c).filter(m->m.getMemberStatus()==MemberStatus.JOINED).isEmpty())throw new ApiException(ErrorCode.BOOKING_STATE_INVALID);o.setGroupBookingId(g.getId());}
                case CORPORATE->{var b=corporate.findByReservationId(r.getId()).orElseThrow();if(!b.getOrganizerCustomerId().equals(c)&&guests.findByCorporateBookingIdAndCustomerId(b.getId(),c).filter(g->g.getStatus()==GuestStatus.JOINED).isEmpty())throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);o.setCorporateBookingId(b.getId());}
            }
        }
        for(var guard:lifecycleGuards)guard.beforeCreate(o.getReservationId(),o.getDiniTicketId());
        var prepared=new ArrayList<Prepared>();BigDecimal gross=BigDecimal.ZERO,net=BigDecimal.ZERO;
        for(var request:q.items()) {
            var m=menu.findByIdAndRestaurantIdAndDeletedAtIsNull(request.menuItemId(),q.restaurantId()).orElseThrow(()->new ApiException(ErrorCode.MENU_ITEM_UNAVAILABLE));
            if(!m.isAvailable()||!m.getCategory().isActive()||m.getCategory().getDeletedAt()!=null||(takeaway&&!m.isTakeawayAvailable())||(q.orderType()==OrderType.DINE_IN_PREORDER&&!m.isPreOrderAvailable())||(q.orderType()==OrderType.DINITICKET_PREORDER&&!m.isDiniTicketPreOrderAvailable()))throw new ApiException(ErrorCode.MENU_ITEM_UNAVAILABLE);
            var v=request.variantId()==null?null:variants.findById(request.variantId()).filter(x->x.getMenuItem().getId().equals(m.getId())&&x.isActive()&&x.getDeletedAt()==null).orElseThrow(()->new ApiException(ErrorCode.MENU_ITEM_UNAVAILABLE));
            if(new HashSet<>(request.addonIds()).size()!=request.addonIds().size())throw new ApiException(ErrorCode.VALIDATION_FAILED);
            var selected=new ArrayList<com.bookeat.menu.entity.MenuAddon>();for(UUID id:request.addonIds())selected.add(addons.findById(id).filter(x->x.getMenuItem().getId().equals(m.getId())&&x.isActive()&&x.getDeletedAt()==null).orElseThrow(()->new ApiException(ErrorCode.MENU_ITEM_UNAVAILABLE)));
            var prices=selected.stream().map(a->a.getPrice()).toList();var selling=MenuPricing.unitPrice(m.getBasePrice(),m.getDiscountPrice(),v==null?null:v.getPrice(),prices);var original=MenuPricing.unitPrice(m.getBasePrice(),null,v==null?null:v.getPrice(),prices);
            var line=new FoodOrderItem();line.setMenuItemId(m.getId());line.setVariantId(v==null?null:v.getId());line.setVariantName(v==null?null:v.getName());line.setItemName(m.getName());line.setImageUrl(m.getImageUrl());line.setQuantity(request.quantity());line.setBaseUnitPrice(original);line.setUnitPrice(selling);line.setSubtotal(selling.multiply(BigDecimal.valueOf(request.quantity())));line.setNotes(request.notes());prepared.add(new Prepared(line,selected));gross=gross.add(original.multiply(BigDecimal.valueOf(request.quantity())));net=net.add(line.getSubtotal());
        }
        BigDecimal configuredTax=orderSettings.findByRestaurantId(o.getRestaurantId()).map(RestaurantOrderSettings::getTaxPercent).orElse(BigDecimal.ZERO);
        // DiniBee food checkout displays 5% food GST. Keep the server as the source of truth
        // so Razorpay receives subtotal + GST (and later any verified coupon discount).
        if((takeaway || q.orderType()==OrderType.DINITICKET_PREORDER) && configuredTax.signum()<=0) configuredTax=new BigDecimal("5.00");
        var totals=OrderPricing.totals(gross,net,configuredTax);o.setSubtotal(totals.subtotal());o.setDiscount(totals.discount());o.setTaxPercent(totals.taxPercent());o.setTax(totals.tax());o.setTotal(totals.total());
        if(o.getCorporateBookingId()!=null){var b=corporate.findById(o.getCorporateBookingId()).orElseThrow();BigDecimal spent=committed(orders.findByCorporateBookingId(b.getId()));if(spent.add(o.getTotal()).compareTo(b.getBudgetLimit())>0)throw new ApiException(ErrorCode.BUDGET_EXCEEDED);}
        orders.saveAndFlush(o);for(var p:prepared){p.line().setOrderId(o.getId());lines.saveAndFlush(p.line());for(var a:p.addons()){var snap=new FoodOrderItemAddon();snap.setOrderItemId(p.line().getId());snap.setMenuAddonId(a.getId());snap.setName(a.getName());snap.setPrice(a.getPrice());lineAddons.save(snap);}}
        changed(o,actor,null,"Order placed; prices snapshotted by server");return o;
    }

    private String nextShortOrderNumber(){
        // Public food-order IDs are four uppercase letters/numbers, e.g. A7K2.
        // UUID remains the internal primary key; this code is only customer/owner facing.
        final String alphabet="ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        for(int attempt=0;attempt<200;attempt++){
            String raw=UUID.randomUUID().toString().replace("-","").toUpperCase(Locale.ROOT);
            StringBuilder code=new StringBuilder(4);
            for(int i=0;i<4;i++){
                int n=Character.digit(raw.charAt(i),16);
                code.append(alphabet.charAt((n + raw.charAt(raw.length()-1-i)) % alphabet.length()));
            }
            String value=code.toString();
            if(!orders.existsByOrderNumber(value)) return value;
        }
        throw new ApiException(ErrorCode.CONFLICT);
    }
    private record Prepared(FoodOrderItem line,List<com.bookeat.menu.entity.MenuAddon> addons) {}
    @Transactional(readOnly=true) public OrderResponses.Order get(UUID actor,UUID id){return mapper.order(owned(actor,id));}
    @Transactional(readOnly=true) public PageResponse<OrderResponses.Order> list(UUID actor,int page,int size){return PageResponse.from(orders.findByCustomerId(access.customer(actor),PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt","id"))).map(mapper::order));}
    @Transactional(readOnly=true) public PageResponse<OrderResponses.Order> ownerList(UUID actor,UUID rid,int page,int size){owners.getOwnedRestaurant(actor,rid);return PageResponse.from(orders.findByRestaurantId(rid,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt","id"))).map(mapper::order));}
    @Transactional(readOnly=true) public OrderResponses.Order ownerGet(UUID actor,UUID rid,UUID id){owners.getOwnedRestaurant(actor,rid);return mapper.order(orders.findById(id).filter(o->o.getRestaurantId().equals(rid)).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND)));}
    public OrderResponses.Order cancel(UUID actor,UUID id,OrderRequests.Action q){access.lock(orders.restaurantId(id).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND)));var o=owned(actor,id);access.version(o.getVersion(),q.version());if(isTakeaway(o))throw new ApiException(ErrorCode.ORDER_STATE_INVALID);transitionLocked(o,actor,FoodOrderStatus.CANCELLED,q.reason(),false);return mapper.order(o);}
    public OrderResponses.Order ownerAction(UUID actor,UUID rid,UUID id,OrderRequests.Action q,FoodOrderStatus target){var restaurant=access.lockOwner(actor,rid);var o=orders.findById(id).filter(e->e.getRestaurantId().equals(rid)).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));access.version(o.getVersion(),q.version());if(isTakeaway(o))throw new ApiException(ErrorCode.ORDER_STATE_INVALID);if(target==FoodOrderStatus.ACCEPTED)access.requireService(restaurant,o.getOrderType()==OrderType.DINITICKET_PREORDER?ServiceType.DINITICKET:ServiceType.DINE_IN);transitionLocked(o,actor,target,q.reason(),false);return mapper.order(o);}
    @Transactional(propagation=Propagation.MANDATORY) public void transitionLocked(FoodOrder o,UUID actor,FoodOrderStatus target,String reason,boolean takeaway){if(o.getStatus()==target)return;for(var guard:lifecycleGuards)guard.beforeTransition(o,target);if(!OrderRules.transition(o.getStatus(),target,takeaway))throw new ApiException(ErrorCode.ORDER_STATE_INVALID);
        if(target==FoodOrderStatus.SERVED&&o.getReservationId()!=null){var r=reservations.findById(o.getReservationId()).orElseThrow();if(r.getStatus()!=ReservationStatus.ARRIVED&&r.getStatus()!=ReservationStatus.CHECKED_IN&&r.getStatus()!=ReservationStatus.DINING)throw new ApiException(ErrorCode.BOOKING_STATE_INVALID);}
        if(target==FoodOrderStatus.SERVED&&o.getDiniTicketId()!=null)for(var a:ticketAuthorizers)a.requireSeated(o.getDiniTicketId());
        var old=o.getStatus();o.setStatus(target);changed(o,actor,old,reason);}
    private void changed(FoodOrder o,UUID actor,FoodOrderStatus old,String reason){var h=new OrderStatusHistory();h.setOrderId(o.getId());h.setActorId(actor);h.setOldStatus(old);h.setNewStatus(o.getStatus());h.setReason(reason);h.setChangedAt(clock.instant());history.save(h);audit.record(actor,"FOOD_ORDER_"+o.getStatus(),"FoodOrder",o.getId(),null);orders.flush();}
    private FoodOrder owned(UUID actor,UUID id){return orders.findById(id).filter(o->o.getCustomerId().equals(access.customer(actor))).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));}
    public static boolean isTakeaway(FoodOrder o){return o.getOrderType()==OrderType.TAKEAWAY||o.getOrderType()==OrderType.ON_THE_WAY;}
    public static BigDecimal committed(List<FoodOrder> orders){return orders.stream().filter(o->o.getStatus()!=FoodOrderStatus.CANCELLED&&o.getStatus()!=FoodOrderStatus.REJECTED).map(FoodOrder::getTotal).reduce(BigDecimal.ZERO,BigDecimal::add);}
    @Transactional(readOnly=true) public OrderResponses.Combined groupSummary(UUID actor,UUID id){var g=groups.findById(id).filter(x->x.getHostCustomerId().equals(access.customer(actor))).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));var list=orders.findByGroupBookingId(id);var total=committed(list);var shares=new ArrayList<OrderResponses.Share>();
        if(g.getPaymentMode()==GroupPaymentMode.HOST_PAYS)shares.add(new OrderResponses.Share(g.getHostCustomerId(),total));else if(g.getPaymentMode()==GroupPaymentMode.SPLIT_EQUAL){var joined=members.findByGroupBookingIdOrderByJoinedAtAscIdAsc(id).stream().filter(m->m.getMemberStatus()==MemberStatus.JOINED).sorted(Comparator.comparing(GroupMember::getId)).toList();var amounts=OrderRules.splitEqual(total,joined.size());for(int i=0;i<joined.size();i++)shares.add(new OrderResponses.Share(joined.get(i).getCustomerId(),amounts.get(i)));}else{var sums=new TreeMap<UUID,BigDecimal>();for(var o:list)if(o.getStatus()!=FoodOrderStatus.CANCELLED&&o.getStatus()!=FoodOrderStatus.REJECTED)sums.merge(o.getCustomerId(),o.getTotal(),BigDecimal::add);sums.forEach((c,amount)->shares.add(new OrderResponses.Share(c,amount)));}
        return new OrderResponses.Combined(id,list.stream().map(mapper::order).toList(),total,"INR",shares,g.getPaymentMode().name());}
    @Transactional(readOnly=true) public OrderResponses.Combined corporateSummary(UUID actor,UUID id){corporate.findById(id).filter(b->b.getOrganizerCustomerId().equals(access.customer(actor))).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));var list=orders.findByCorporateBookingId(id);return new OrderResponses.Combined(id,list.stream().map(mapper::order).toList(),committed(list),"INR",List.of(),"COMPANY_BILLING_INTENT");}
}
