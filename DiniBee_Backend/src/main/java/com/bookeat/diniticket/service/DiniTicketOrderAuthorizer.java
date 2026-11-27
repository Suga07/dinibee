package com.bookeat.diniticket.service;
import com.bookeat.order.service.TicketOrderAuthorizer;
import com.bookeat.diniticket.repository.DiniTicketRepository;
import com.bookeat.diniticket.enums.TicketStatus;
import com.bookeat.restaurant.repository.DiniTicketSettingsRepository;
import com.bookeat.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import java.time.Clock;
import java.util.UUID;
@Component @RequiredArgsConstructor @Transactional(propagation=Propagation.MANDATORY)
public class DiniTicketOrderAuthorizer implements TicketOrderAuthorizer {
    private final DiniTicketRepository tickets;private final DiniTicketSettingsRepository settings;private final Clock clock;
    public void authorize(UUID customerId,UUID restaurantId,UUID ticketId){var t=tickets.findById(ticketId).filter(x->x.getCustomerId().equals(customerId)&&x.getRestaurantId().equals(restaurantId)).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));var cfg=settings.findByRestaurantId(restaurantId).orElseThrow(()->new ApiException(ErrorCode.SERVICE_NOT_AVAILABLE));if(!cfg.isEnabled()||!cfg.isAllowPreOrder()||!TicketRules.QUEUED.contains(t.getStatus())||(cfg.isAutoExpireTickets()&&!clock.instant().isBefore(t.getExpiresAt())))throw new ApiException(ErrorCode.TICKET_STATE_INVALID);}
    public void requireSeated(UUID id){if(tickets.findById(id).filter(t->t.getStatus()==TicketStatus.SEATED).isEmpty())throw new ApiException(ErrorCode.TICKET_STATE_INVALID);}
}
