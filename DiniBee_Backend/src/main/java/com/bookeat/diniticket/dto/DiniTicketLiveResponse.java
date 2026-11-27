package com.bookeat.diniticket.dto;

import java.util.List;

/**
 * Server-authoritative live queue snapshot for the Restaurant Partner app.
 * Current token is selected on the backend from persisted DiniTickets so the
 * owner UI never derives a different "now serving" token from stale local data.
 */
public record DiniTicketLiveResponse(
        TicketResponse current,
        List<TicketResponse> next,
        long waitingCount,
        long calledCount,
        long arrivedCount,
        long seatedCount) {
}
