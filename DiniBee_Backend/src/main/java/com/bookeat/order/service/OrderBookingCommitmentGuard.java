package com.bookeat.order.service;
import com.bookeat.order.repository.FoodOrderRepository;
import com.bookeat.order.enums.FoodOrderStatus;
import com.bookeat.reservation.service.BookingCommitmentGuard;
import com.bookeat.reservation.entity.Reservation;
import com.bookeat.exception.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
import java.math.BigDecimal;
@Component @RequiredArgsConstructor @Transactional(propagation=Propagation.MANDATORY)
public class OrderBookingCommitmentGuard implements BookingCommitmentGuard {
    private final FoodOrderRepository orders;private final OrderService service;
    public void beforeReschedule(Reservation r){if(orders.findByReservationId(r.getId()).stream().anyMatch(o->!OrderRules.terminal(o.getStatus())))throw new ApiException(ErrorCode.ORDER_IN_PROGRESS);}
    public void beforeCancel(Reservation r,UUID actor){var list=orders.findByReservationId(r.getId());if(list.stream().anyMatch(o->!OrderRules.terminal(o.getStatus())&&o.getStatus()!=FoodOrderStatus.PLACED))throw new ApiException(ErrorCode.ORDER_IN_PROGRESS);for(var o:list)if(o.getStatus()==FoodOrderStatus.PLACED)service.transitionLocked(o,actor,FoodOrderStatus.CANCELLED,"Linked reservation cancelled",false);}
    public void beforeMemberRemoval(UUID groupId,UUID customerId){if(orders.findByGroupBookingId(groupId).stream().anyMatch(o->o.getCustomerId().equals(customerId)&&!OrderRules.terminal(o.getStatus())))throw new ApiException(ErrorCode.ORDER_IN_PROGRESS);}
    public void checkCorporateBudget(UUID id,BigDecimal budget){if(OrderService.committed(orders.findByCorporateBookingId(id)).compareTo(budget)>0)throw new ApiException(ErrorCode.BUDGET_EXCEEDED);}
}
