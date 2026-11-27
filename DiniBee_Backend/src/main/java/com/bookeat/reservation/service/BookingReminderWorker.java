package com.bookeat.reservation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Automatically sends booking reminder notifications 30 minutes before
 * the scheduled reservation time to all guests (host + all joined group members).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingReminderWorker {
    private final JdbcTemplate jdbc;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${app.booking.reminder-poll-ms:60000}")
    public void sendUpcomingReminders() {
        try {
            Instant now = clock.instant();
            // Window of bookings starting between 25 and 35 minutes from now
            Instant windowStart = now.plus(25, ChronoUnit.MINUTES);
            Instant windowEnd = now.plus(35, ChronoUnit.MINUTES);

            List<Map<String, Object>> upcomingReservations = jdbc.queryForList(
                "SELECT r.id, r.booking_reference, r.guest_count, r.reservation_date, r.start_time, " +
                "r.booking_type, r.customer_id, rest.restaurant_name " +
                "FROM reservations r " +
                "JOIN restaurants rest ON rest.id = r.restaurant_id " +
                "WHERE r.status = 'CONFIRMED' " +
                "AND r.starts_at >= ? AND r.starts_at <= ?",
                java.sql.Timestamp.from(windowStart),
                java.sql.Timestamp.from(windowEnd)
            );

            for (Map<String, Object> res : upcomingReservations) {
                UUID resId = (UUID) res.get("id");
                String ref = res.get("booking_reference") != null ? res.get("booking_reference").toString() : "";
                String guestCount = res.get("guest_count") != null ? res.get("guest_count").toString() : "2";
                String time = res.get("start_time") != null ? res.get("start_time").toString() : "";
                String restName = res.get("restaurant_name") != null ? res.get("restaurant_name").toString() : "Restaurant";
                String bookingType = res.get("booking_type") != null ? res.get("booking_type").toString() : "REGULAR";

                Set<UUID> targetUserIds = new HashSet<>();

                if ("GROUP".equalsIgnoreCase(bookingType)) {
                    // Fetch all joined group members + host
                    List<UUID> groupUsers = jdbc.query(
                        "SELECT c.user_id FROM group_bookings gb " +
                        "JOIN group_members gm ON gm.group_booking_id = gb.id " +
                        "JOIN customers c ON c.id = gm.customer_id " +
                        "WHERE gb.reservation_id = ? AND gm.member_status = 'JOINED'",
                        (rs, n) -> rs.getObject(1, UUID.class), resId
                    );
                    targetUserIds.addAll(groupUsers);

                    List<UUID> hostUser = jdbc.query(
                        "SELECT c.user_id FROM group_bookings gb " +
                        "JOIN customers c ON c.id = gb.host_customer_id " +
                        "WHERE gb.reservation_id = ?",
                        (rs, n) -> rs.getObject(1, UUID.class), resId
                    );
                    targetUserIds.addAll(hostUser);
                } else {
                    List<UUID> custUser = jdbc.query(
                        "SELECT c.user_id FROM reservations r JOIN customers c ON c.id = r.customer_id WHERE r.id = ?",
                        (rs, n) -> rs.getObject(1, UUID.class), resId
                    );
                    targetUserIds.addAll(custUser);
                }

                String title = "⏰ Booking Reminder: Upcoming in 30 Mins!";
                String message = "Your table reservation #" + ref + " at " + restName + " for " + guestCount + 
                                 " guests is scheduled in 30 minutes (at " + time + "). Table is reserved and ready!";

                for (UUID userId : targetUserIds) {
                    if (userId == null) continue;
                    String dedup = "REMINDER_30_MIN:" + resId + ":" + userId;
                    jdbc.update(
                        "INSERT INTO notifications(id, user_id, title, message, notification_type, reference_type, reference_id, read_flag, deduplication_key) " +
                        "VALUES(?,?,?,?,?,'Reservation',?,false,?) ON CONFLICT(user_id, deduplication_key) DO NOTHING",
                        UUID.randomUUID(), userId, title, message, "BOOKING_REMINDER_30_MIN", resId, dedup
                    );
                }
            }
        } catch (Exception ex) {
            log.warn("Booking reminder worker poll skipped due to error: {}", ex.getMessage());
        }
    }
}
