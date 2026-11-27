package com.bookeat.order.service;
import com.bookeat.diniticket.service.TicketCommitmentGuard;
import com.bookeat.order.repository.FoodOrderRepository;
import com.bookeat.order.enums.FoodOrderStatus;
import com.bookeat.exception.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
@Component @RequiredArgsConstructor @Transactional(propagation=Propagation.MANDATORY)
public class OrderTicketCommitmentGuard implements TicketCommitmentGuard {
    private final FoodOrderRepository orders;private final OrderService service;private final JdbcTemplate jdbc;

    public boolean mayExpire(UUID id){return orders.findByDiniTicketId(id).stream().noneMatch(o->!OrderRules.terminal(o.getStatus())&&o.getStatus()!=FoodOrderStatus.PLACED);}

    public void beforeCancel(UUID id,UUID actor){
        for(var o:orders.findByDiniTicketId(id)){
            if(OrderRules.terminal(o.getStatus()))continue;

            // A Pay-at-Restaurant DiniTicket reserves a CASH payment in PENDING
            // state.  That unpaid intent must not prevent an owner from cancelling
            // or marking the ticket No Show.  Close only unresolved CASH intents;
            // captured online payments remain protected by PaymentBusinessGuard
            // and still require the normal refund flow.
            jdbc.update("""
                update payments p
                   set status='FAILED',
                       failure_reason='DiniTicket cancelled/no-show before cash collection',
                       version=version+1,
                       updated_at=CURRENT_TIMESTAMP
                 where p.gateway='CASH'
                   and p.status in ('CREATED','PENDING')
                   and exists (
                       select 1 from payment_lines l
                        where l.payment_id=p.id and l.food_order_id=?
                   )
                """,o.getId());

            if(o.getStatus()==FoodOrderStatus.PLACED){
                service.transitionLocked(o,actor,FoodOrderStatus.CANCELLED,"Linked DiniTicket cancelled/no-show",false);
            }else if(o.getStatus()==FoodOrderStatus.ACCEPTED||o.getStatus()==FoodOrderStatus.PREPARING||o.getStatus()==FoodOrderStatus.READY){
                service.transitionLocked(o,actor,FoodOrderStatus.REJECTED,"Linked DiniTicket cancelled/no-show",false);
            }else{
                // SERVED/other committed work cannot be silently discarded.
                throw new ApiException(ErrorCode.ORDER_IN_PROGRESS);
            }
        }
    }

    public void beforeComplete(UUID id){if(orders.findByDiniTicketId(id).stream().anyMatch(o->!OrderRules.terminal(o.getStatus())))throw new ApiException(ErrorCode.ORDER_IN_PROGRESS);}
}
