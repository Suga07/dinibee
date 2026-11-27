package com.bookeat.notification.service;

import com.bookeat.common.audit.AuditListener;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationAuditListener implements AuditListener {
    private final JdbcTemplate jdbc;

    @Override
    public void recorded(UUID actor, String action, String entity, UUID id, UUID auditId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    processRecorded(actor, action, entity, id, auditId);
                }
            });
        } else {
            processRecorded(actor, action, entity, id, auditId);
        }
    }

    private void processRecorded(UUID actor, String action, String entity, UUID id, UUID auditId) {
        try {
            if ("FoodOrder".equals(entity)) {
                handleFoodOrder(actor, action, id, auditId);
                return;
            }
            if ("Reservation".equals(entity)) {
                handleReservation(actor, action, id, auditId);
                return;
            }
            if ("DiniTicket".equals(entity)) {
                handleDiniTicket(actor, action, id, auditId);
                return;
            }
            if ("Restaurant".equals(entity) && (action.contains("APPROVED") || action.contains("REJECTED"))) {
                List<UUID> owners = jdbc.query(
                    "select o.user_id from restaurants r join restaurant_owners o on o.id=r.owner_id where r.id=?",
                    (r, n) -> r.getObject(1, UUID.class), id);
                String msg = action.contains("APPROVED") ? "Your restaurant profile has been approved!" : "Your restaurant profile was rejected. Please review feedback.";
                for (UUID u : owners) {
                    insertNotification(u, "Restaurant Status", msg, action, entity, id, auditId);
                }
                return;
            }
            if ("Payment".equals(entity) && ("PAYMENT_SUCCESS".equals(action) || "PAYMENT_CASH_CONFIRMED".equals(action))) {
                handlePaymentConfirmed(actor, action, id, auditId);
                return;
            }
            if ("Refund".equals(entity) && "REFUND_COMPLETED".equals(action)) {
                List<UUID> customers = jdbc.query(
                    "select c.user_id from refunds r join payments p on p.id=r.payment_id join customers c on c.id=p.customer_id where r.id=?",
                    (r, n) -> r.getObject(1, UUID.class), id);
                for (UUID u : customers) {
                    insertNotification(u, "Refund Processed", "Your refund has been completed successfully.", action, entity, id, auditId);
                }
                return;
            }
            if ("Settlement".equals(entity) && "SETTLEMENT_COMPLETED".equals(action)) {
                List<UUID> owners = jdbc.query(
                    "select o.user_id from settlements s join restaurants r on r.id=s.restaurant_id join restaurant_owners o on o.id=r.owner_id where s.id=?",
                    (r, n) -> r.getObject(1, UUID.class), id);
                for (UUID u : owners) {
                    insertNotification(u, "Settlement Processed", "Your payout settlement has been completed.", action, entity, id, auditId);
                }
                return;
            }
            if ("WalletTransaction".equals(entity) && "REWARD_POINTS_EARNED".equals(action)) {
                List<UUID> customers = jdbc.query(
                    "select c.user_id from wallet_transactions t join wallets w on w.id=t.wallet_id join customers c on c.id=w.customer_id where t.id=?",
                    (r, n) -> r.getObject(1, UUID.class), id);
                for (UUID u : customers) {
                    insertNotification(u, "Points Earned!", "You earned reward points on your latest transaction!", action, entity, id, auditId);
                }
                return;
            }
            if ("SupportTicket".equals(entity) && "SUPPORT_REPLY".equals(action)) {
                List<UUID> users = jdbc.query(
                    "select created_by_user from support_tickets where id=?",
                    (r, n) -> r.getObject(1, UUID.class), id);
                for (UUID u : users) {
                    if (!u.equals(actor)) {
                        insertNotification(u, "Support Ticket Update", "A new reply has been posted to your support ticket.", action, entity, id, auditId);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Error processing audit notification for entity: {}, action: {}, id: {}", entity, action, id, ex);
        }
    }

    private void handleFoodOrder(UUID actor, String action, UUID orderId, UUID auditId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "select o.order_number, o.order_type, o.total, r.restaurant_name " +
            "from food_orders o join restaurants r on r.id=o.restaurant_id where o.id=?", orderId);
        Map<String, Object> orderMap = rows.isEmpty() ? null : rows.get(0);

        String orderNumber = orderMap != null && orderMap.get("order_number") != null ? orderMap.get("order_number").toString() : "";
        String orderType = orderMap != null && orderMap.get("order_type") != null ? orderMap.get("order_type").toString() : "ORDER";
        String total = orderMap != null && orderMap.get("total") != null ? orderMap.get("total").toString() : "";
        String restName = orderMap != null && orderMap.get("restaurant_name") != null ? orderMap.get("restaurant_name").toString() : "Restaurant";

        boolean isTakeaway = "TAKEAWAY".equals(orderType) || "ON_THE_WAY".equals(orderType);
        boolean isDiniTicket = "DINITICKET_PREORDER".equals(orderType);

        // Fetch customer user id
        List<UUID> customerUsers = jdbc.query(
            "select c.user_id from food_orders o join customers c on c.id=o.customer_id where o.id=?",
            (r, n) -> r.getObject(1, UUID.class), orderId);

        // Fetch owner user id
        List<UUID> ownerUsers = jdbc.query(
            "select o.user_id from food_orders fo join restaurants r on r.id=fo.restaurant_id join restaurant_owners o on o.id=r.owner_id where fo.id=?",
            (r, n) -> r.getObject(1, UUID.class), orderId);

        switch (action) {
            case "FOOD_ORDER_PLACED": {
                java.math.BigDecimal orderTotal = java.math.BigDecimal.ZERO;
                if (orderMap != null && orderMap.get("total") != null) {
                    try {
                        orderTotal = new java.math.BigDecimal(orderMap.get("total").toString());
                    } catch (Exception ignored) {}
                }
                boolean isZeroAmount = orderTotal.compareTo(java.math.BigDecimal.ZERO) <= 0;

                java.math.BigDecimal paid = jdbc.queryForObject(
                    "select coalesce(sum(l.amount), 0) from payment_lines l join payments p on p.id=l.payment_id where l.food_order_id=? and p.status in ('SUCCESS','PARTIALLY_REFUNDED','REFUNDED')",
                    java.math.BigDecimal.class, orderId);
                boolean isPaid = paid != null && paid.compareTo(orderTotal) >= 0;

                Integer cashCount = jdbc.queryForObject(
                    "select count(*) from payment_lines l join payments p on p.id=l.payment_id where l.food_order_id=? and p.gateway='CASH' and p.status in ('PENDING','SUCCESS')",
                    Integer.class, orderId);
                boolean isCash = cashCount != null && cashCount > 0;

                // Send placed notifications ONLY for:
                // 1. Free orders (total <= 0)
                // 2. Orders that have already been paid in full (isPaid)
                // 3. Orders with confirmed CASH (isCash)
                // Online orders waiting for Razorpay payment MUST NEVER trigger placed notifications here;
                // their notifications are deferred until PAYMENT_SUCCESS in handlePaymentConfirmed.
                if (isZeroAmount || isPaid || isCash) {
                    sendOrderPlacedNotifications(actor, orderId, auditId, isCash);
                }
                break;
            }
            case "FOOD_ORDER_ACCEPTED": {
                String type = isDiniTicket ? "DINITICKET_ORDER_CONFIRMED" : "FOOD_ORDER_ACCEPTED";
                for (UUID c : customerUsers) {
                    insertNotification(c, "Order Confirmed!",
                        "Your order #" + orderNumber + " has been accepted by " + restName + ".",
                        type, "FoodOrder", orderId, auditId);
                }
                break;
            }
            case "FOOD_ORDER_PREPARING": {
                String type = isDiniTicket ? "DINITICKET_PREPARING" : "FOOD_ORDER_PREPARING";
                for (UUID c : customerUsers) {
                    insertNotification(c, "Order Preparing",
                        "The kitchen is now preparing your order #" + orderNumber + ".",
                        type, "FoodOrder", orderId, auditId);
                }
                break;
            }
            case "FOOD_ORDER_READY": {
                String type = isDiniTicket ? "DINITICKET_READY" : (isTakeaway ? "TAKEAWAY_READY" : "FOOD_ORDER_READY");
                String title = isTakeaway ? "Takeaway Order Ready!" : "Order Ready!";
                String msg = isTakeaway
                    ? "Your takeaway order #" + orderNumber + " is packed and ready for pickup at " + restName + "!"
                    : "Your order #" + orderNumber + " is ready at " + restName + "!";
                for (UUID c : customerUsers) {
                    insertNotification(c, title, msg, type, "FoodOrder", orderId, auditId);
                }
                break;
            }
            case "FOOD_ORDER_SERVED": {
                for (UUID c : customerUsers) {
                    insertNotification(c, "Order Served",
                        "Your order #" + orderNumber + " has been served. Enjoy your meal!",
                        "FOOD_ORDER_SERVED", "FoodOrder", orderId, auditId);
                }
                break;
            }
            case "FOOD_ORDER_COMPLETED": {
                // Completed / Finished order
                String type = isDiniTicket ? "DINITICKET_COMPLETED" : (isTakeaway ? "TAKEAWAY_COLLECTED" : "FOOD_ORDER_COMPLETED");
                for (UUID c : customerUsers) {
                    insertNotification(c, "Order Completed!",
                        "Your order #" + orderNumber + " at " + restName + " is completed. Thank you for dining with DiniBee!",
                        type, "FoodOrder", orderId, auditId);
                }
                for (UUID o : ownerUsers) {
                    insertNotification(o, "Order Finished",
                        "Order #" + orderNumber + " has been completed and closed.",
                        "FOOD_ORDER_COMPLETED", "FoodOrder", orderId, auditId);
                }
                break;
            }
            case "FOOD_ORDER_CANCELLED": {
                for (UUID c : customerUsers) {
                    insertNotification(c, "Order Cancelled",
                        "Your order #" + orderNumber + " has been cancelled.",
                        "FOOD_ORDER_CANCELLED", "FoodOrder", orderId, auditId);
                }
                for (UUID o : ownerUsers) {
                    insertNotification(o, "Order Cancelled",
                        "Order #" + orderNumber + " was cancelled.",
                        "FOOD_ORDER_CANCELLED", "FoodOrder", orderId, auditId);
                }
                break;
            }
            case "FOOD_ORDER_REJECTED": {
                for (UUID c : customerUsers) {
                    insertNotification(c, "Order Declined",
                        "Your order #" + orderNumber + " could not be accepted by " + restName + ".",
                        "FOOD_ORDER_REJECTED", "FoodOrder", orderId, auditId);
                }
                break;
            }
            default:
                break;
        }
    }

    private void handleReservation(UUID actor, String action, UUID resId, UUID auditId) {
        List<Map<String, Object>> resRows = jdbc.queryForList(
            "select r.booking_reference, r.guest_count, r.reservation_date, r.start_time, rest.restaurant_name " +
            "from reservations r join restaurants rest on rest.id=r.restaurant_id where r.id=?", resId);
        Map<String, Object> resMap = resRows.isEmpty() ? null : resRows.get(0);

        String ref = resMap != null && resMap.get("booking_reference") != null ? resMap.get("booking_reference").toString() : "";
        String guests = resMap != null && resMap.get("guest_count") != null ? resMap.get("guest_count").toString() : "";
        String date = resMap != null && resMap.get("reservation_date") != null ? resMap.get("reservation_date").toString() : "";
        String time = resMap != null && resMap.get("start_time") != null ? resMap.get("start_time").toString() : "";
        String restName = resMap != null && resMap.get("restaurant_name") != null ? resMap.get("restaurant_name").toString() : "Restaurant";

        List<UUID> customerUsers = jdbc.query(
            "select c.user_id from reservations r join customers c on c.id=r.customer_id where r.id=?",
            (r, n) -> r.getObject(1, UUID.class), resId);

        List<UUID> ownerUsers = jdbc.query(
            "select o.user_id from reservations res join restaurants r on r.id=res.restaurant_id join restaurant_owners o on o.id=r.owner_id where res.id=?",
            (r, n) -> r.getObject(1, UUID.class), resId);

        switch (action) {
            case "RESERVATION_PENDING": {
                for (UUID c : customerUsers) {
                    insertNotification(c, "Booking Request Placed",
                        "Your booking request #" + ref + " at " + restName + " for " + guests + " guests is pending confirmation.",
                        "RESERVATION_PENDING", "Reservation", resId, auditId);
                }
                for (UUID o : ownerUsers) {
                    insertNotification(o, "🔔 New Table Booking Request!",
                        "New booking request #" + ref + " for " + guests + " guests on " + date + " at " + time + ".",
                        "RESERVATION_PENDING", "Reservation", resId, auditId);
                }
                break;
            }
            case "RESERVATION_CONFIRMED": {
                for (UUID c : customerUsers) {
                    insertNotification(c, "Booking Confirmed!",
                        "Your table booking #" + ref + " at " + restName + " is confirmed for " + date + " at " + time + "!",
                        "RESERVATION_CONFIRMED", "Reservation", resId, auditId);
                }
                break;
            }
            case "RESERVATION_COMPLETED": {
                for (UUID c : customerUsers) {
                    insertNotification(c, "Dining Completed",
                        "Your dining session #" + ref + " at " + restName + " is completed. Thank you for visiting!",
                        "RESERVATION_COMPLETED", "Reservation", resId, auditId);
                }
                for (UUID o : ownerUsers) {
                    insertNotification(o, "Booking Finished",
                        "Table reservation #" + ref + " is completed and the table is now free.",
                        "RESERVATION_COMPLETED", "Reservation", resId, auditId);
                }
                break;
            }
            case "RESERVATION_CANCELLED": {
                for (UUID c : customerUsers) {
                    insertNotification(c, "Booking Cancelled",
                        "Your table booking #" + ref + " has been cancelled.",
                        "RESERVATION_CANCELLED", "Reservation", resId, auditId);
                }
                for (UUID o : ownerUsers) {
                    insertNotification(o, "Booking Cancelled",
                        "Booking #" + ref + " has been cancelled.",
                        "RESERVATION_CANCELLED", "Reservation", resId, auditId);
                }
                break;
            }
            case "RESERVATION_REJECTED": {
                for (UUID c : customerUsers) {
                    insertNotification(c, "Booking Declined",
                        "Your table booking request #" + ref + " could not be accepted by " + restName + ".",
                        "RESERVATION_REJECTED", "Reservation", resId, auditId);
                }
                break;
            }
            default:
                break;
        }
    }

    private void handleDiniTicket(UUID actor, String action, UUID ticketId, UUID auditId) {
        List<UUID> customerUsers = jdbc.query(
            "select c.user_id from dini_tickets t join customers c on c.id=t.customer_id where t.id=?",
            (r, n) -> r.getObject(1, UUID.class), ticketId);

        List<UUID> ownerUsers = jdbc.query(
            "select o.user_id from dini_tickets t join restaurants r on r.id=t.restaurant_id join restaurant_owners o on o.id=r.owner_id where t.id=?",
            (r, n) -> r.getObject(1, UUID.class), ticketId);

        String type;
        String title;
        String msg;

        switch (action) {
            case "TICKET_CALLED":
                type = "DINITICKET_CALLED";
                title = "Your Token is Called!";
                msg = "Your DiniTicket token has been called. Please proceed to the restaurant counter.";
                break;
            case "TICKET_ARRIVED":
                type = "DINITICKET_ARRIVED";
                title = "Arrival Confirmed";
                msg = "Your DiniTicket arrival has been confirmed by the restaurant.";
                break;
            case "TICKET_SEATED":
                type = "DINITICKET_HANDED_OVER";
                title = "Order Handed Over";
                msg = "Your DiniTicket order has been handed over. Enjoy!";
                break;
            case "TICKET_COMPLETED":
                type = "DINITICKET_COMPLETED";
                title = "Ticket Completed";
                msg = "Your DiniTicket order is completed. Thank you for using DiniBee.";
                break;
            case "TICKET_CANCELLED":
                type = "DINITICKET_CANCELLED";
                title = "Ticket Cancelled";
                msg = "Your DiniTicket order was cancelled.";
                break;
            case "TICKET_NO_SHOW":
                type = "DINITICKET_NO_SHOW";
                title = "Marked No Show";
                msg = "Your DiniTicket was marked as no show by the restaurant.";
                break;
            case "TICKET_ISSUED":
            case "TICKET_CREATED":
                type = "DINITICKET_ISSUED";
                title = "DiniTicket Issued";
                msg = "Your DiniTicket queue token has been generated.";
                for (UUID o : ownerUsers) {
                    insertNotification(o, "🔔 New DiniTicket Token", "A new queue token has been taken by a customer.", "DINITICKET_ISSUED", "DiniTicket", ticketId, auditId);
                }
                break;
            default:
                type = "DINITICKET_EXPIRED";
                title = "Ticket Expired";
                msg = "Your DiniTicket token has expired.";
                break;
        }

        for (UUID c : customerUsers) {
            insertNotification(c, title, msg, type, "DiniTicket", ticketId, auditId);
        }
    }

    private void handlePaymentConfirmed(UUID actor, String action, UUID paymentId, UUID auditId) {
        boolean isCash = "PAYMENT_CASH_CONFIRMED".equals(action);
        if (!isCash) {
            List<UUID> customers = jdbc.query(
                "select c.user_id from payments p join customers c on c.id=p.customer_id where p.id=?",
                (r, n) -> r.getObject(1, UUID.class), paymentId);
            for (UUID u : customers) {
                insertNotification(u, "Payment Successful", "Your payment was processed successfully. Thank you!", action, "Payment", paymentId, auditId);
            }
        }

        Set<UUID> foodOrderIds = new LinkedHashSet<>(jdbc.query(
            "select distinct l.food_order_id from payment_lines l where l.payment_id=?",
            (r, n) -> r.getObject(1, UUID.class), paymentId));

        List<UUID> takeawayFoodOrders = jdbc.query(
            "select t.food_order_id from payments p join takeaway_orders t on t.id=p.reference_id where p.id=? and p.reference_type='TAKEAWAY'",
            (r, n) -> r.getObject(1, UUID.class), paymentId);
        foodOrderIds.addAll(takeawayFoodOrders);

        for (UUID orderId : foodOrderIds) {
            sendOrderPlacedNotifications(actor, orderId, auditId, isCash);
        }
    }

    private void sendOrderPlacedNotifications(UUID actor, UUID orderId, UUID auditId, boolean isCash) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "select o.order_number, o.order_type, o.total, r.restaurant_name " +
            "from food_orders o join restaurants r on r.id=o.restaurant_id where o.id=?", orderId);
        if (rows.isEmpty()) return;
        Map<String, Object> orderMap = rows.get(0);

        String orderNumber = orderMap.get("order_number") != null ? orderMap.get("order_number").toString() : "";
        String orderType = orderMap.get("order_type") != null ? orderMap.get("order_type").toString() : "ORDER";
        String total = orderMap.get("total") != null ? orderMap.get("total").toString() : "0.00";
        String restName = orderMap.get("restaurant_name") != null ? orderMap.get("restaurant_name").toString() : "Restaurant";

        boolean isTakeaway = "TAKEAWAY".equals(orderType) || "ON_THE_WAY".equals(orderType);

        List<UUID> customerUsers = jdbc.query(
            "select c.user_id from food_orders o join customers c on c.id=o.customer_id where o.id=?",
            (r, n) -> r.getObject(1, UUID.class), orderId);

        List<UUID> ownerUsers = jdbc.query(
            "select o.user_id from food_orders fo join restaurants r on r.id=fo.restaurant_id join restaurant_owners o on o.id=r.owner_id where fo.id=?",
            (r, n) -> r.getObject(1, UUID.class), orderId);

        String custMsg = "Your order #" + orderNumber + " has been placed with " + restName + ". Total: ₹" + total + "." + (isCash ? " (Pay at restaurant)" : "");
        for (UUID c : customerUsers) {
            insertNotification(c, "Order Placed Successfully", custMsg, "FOOD_ORDER_PLACED", "FoodOrder", orderId, auditId);
        }

        String ownerMsg = "New " + (isTakeaway ? "takeaway" : "dine-in") + " order #" + orderNumber + " received. Total: ₹" + total + "." + (isCash ? " (Cash on Pickup)" : "");
        for (UUID o : ownerUsers) {
            insertNotification(o, "🔔 New Order Received!", ownerMsg, "FOOD_ORDER_PLACED", "FoodOrder", orderId, auditId);
        }
    }

    private void insertNotification(UUID userId, String title, String message, String type, String entity, UUID id, UUID auditId) {
        if (userId == null) return;
        try {
            String dedup = type + ":" + id + ":" + userId;
            if (dedup.length() > 200) {
                dedup = dedup.substring(0, 200);
            }
            if (title != null && title.length() > 160) {
                title = title.substring(0, 160);
            }
            if (message != null && message.length() > 2000) {
                message = message.substring(0, 2000);
            }
            jdbc.update(
                "insert into notifications(id, user_id, title, message, notification_type, reference_type, reference_id, read_flag, deduplication_key) " +
                "values(?, ?, ?, ?, ?, ?, ?, false, ?) on conflict(user_id, deduplication_key) do nothing",
                UUID.randomUUID(), userId, title, message, type, entity, id, dedup);
        } catch (Exception ex) {
            log.error("Failed to insert notification for user {}: {}", userId, ex.getMessage());
        }
    }
}
