package com.bookeat.diniticket.service;
import java.util.UUID;
public interface TicketCommitmentGuard {
    boolean mayExpire(UUID ticketId);
    void beforeCancel(UUID ticketId,UUID actor);
    void beforeComplete(UUID ticketId);
}
