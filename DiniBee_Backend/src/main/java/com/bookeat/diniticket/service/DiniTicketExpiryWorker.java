package com.bookeat.diniticket.service;
import com.bookeat.diniticket.repository.DiniTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.domain.PageRequest;
import java.time.Clock;
@Component @RequiredArgsConstructor @Slf4j
public class DiniTicketExpiryWorker {
    private final DiniTicketRepository tickets;
    private final DiniTicketService service;
    private final Clock clock;

    // Default batch size kept small (10) to limit PostgreSQL per-query memory.
    // Override via app.diniticket.expiry-batch-size if the database has more RAM.
    // Default poll interval is 60 s (was 30 s) to reduce scheduler pressure.
    @Scheduled(fixedDelayString = "${app.diniticket.expiry-poll-ms:60000}")
    public void expire() {
        try {
            var ids = tickets.expiringRestaurants(
                    clock.instant(),
                    PageRequest.of(0, 10));
            for (var id : ids) {
                try {
                    service.expireRestaurant(id);
                } catch (RuntimeException e) {
                    log.warn("Ticket expiry deferred restaurant={} type={}",
                            id, e.getClass().getSimpleName());
                }
            }
        } catch (Exception e) {
            // Catching here prevents the Spring scheduler from suppressing
            // future executions of this task after a transient error such as
            // PostgreSQL OOM in a low-memory environment.
            log.warn("DiniTicket expiry poll skipped due to transient error: {}",
                    e.getMessage());
        }
    }
}
