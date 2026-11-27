package com.bookeat.diniticket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiniTicketSessionService {
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public record ActiveSession(UUID id, UUID restaurantId, Instant startedAt, int maxTokens, int issuedCount) {}

    @Transactional
    public ActiveSession ensureActive(UUID restaurantId, int maxTokens) {
        lock(restaurantId);
        var existing = active(restaurantId);
        if (existing != null) return existing;
        // First run after upgrading from the legacy queue: old active rows belong
        // to the previous session and must not block DT-1 of the new session.
        jdbc.update("update dini_tickets set status='EXPIRED' where restaurant_id=? and status in ('WAITING','CALLED','ARRIVED','SEATED')", restaurantId);
        return startNewLocked(restaurantId, maxTokens);
    }

    @Transactional
    public ActiveSession startNew(UUID restaurantId, int maxTokens) {
        lock(restaurantId);
        jdbc.update("update diniticket_sessions set active=false, ended_at=? where restaurant_id=? and active=true",
                Timestamp.from(clock.instant()), restaurantId);
        // Any token from the previous session must not leak into the new live queue.
        jdbc.update("update dini_tickets set status='EXPIRED' where restaurant_id=? and status in ('WAITING','CALLED','ARRIVED','SEATED')",
                restaurantId);
        return startNewLocked(restaurantId, maxTokens);
    }

    @Transactional(readOnly = true)
    public ActiveSession active(UUID restaurantId) {
        var rows = jdbc.query("select id, restaurant_id, started_at, max_tokens, issued_count from diniticket_sessions where restaurant_id=? and active=true order by started_at desc limit 1",
                (rs, n) -> new ActiveSession(rs.getObject("id", UUID.class), rs.getObject("restaurant_id", UUID.class),
                        rs.getTimestamp("started_at").toInstant(), rs.getInt("max_tokens"), rs.getInt("issued_count")), restaurantId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Transactional
    public long nextTokenNumber(UUID restaurantId, int maxTokens) {
        lock(restaurantId);
        var session = active(restaurantId);
        if (session == null) session = startNewLocked(restaurantId, maxTokens);
        if (session.issuedCount() >= session.maxTokens()) throw new com.bookeat.exception.ApiException(com.bookeat.exception.ErrorCode.QUEUE_FULL);
        int next = session.issuedCount() + 1;
        jdbc.update("update diniticket_sessions set issued_count=? where id=?", next, session.id());
        return next;
    }

    @Transactional
    public void closeIfFull(UUID restaurantId) {
        lock(restaurantId);
        var s = active(restaurantId);
        if (s != null && s.issuedCount() >= s.maxTokens()) {
            jdbc.update("update diniticket_sessions set active=false, ended_at=? where id=?", Timestamp.from(clock.instant()), s.id());
        }
    }

    public boolean belongsToActive(UUID restaurantId, Instant issuedAt) {
        var s = active(restaurantId);
        return s != null && issuedAt != null && !issuedAt.isBefore(s.startedAt());
    }

    private ActiveSession startNewLocked(UUID restaurantId, int maxTokens) {
        UUID id = UUID.randomUUID();
        Instant now = clock.instant();
        int limit = Math.max(1, maxTokens);
        jdbc.update("insert into diniticket_sessions(id,restaurant_id,started_at,max_tokens,issued_count,active) values (?,?,?,?,0,true)",
                id, restaurantId, Timestamp.from(now), limit);
        return new ActiveSession(id, restaurantId, now, limit, 0);
    }

    private void lock(UUID restaurantId) {
        jdbc.query("select pg_advisory_xact_lock(hashtext(?::text))", rs -> null, restaurantId.toString());
    }
}
