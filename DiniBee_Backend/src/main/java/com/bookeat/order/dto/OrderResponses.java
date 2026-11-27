package com.bookeat.order.dto;
import com.bookeat.order.enums.*;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
public final class OrderResponses {
    private OrderResponses() {}
    public record Addon(UUID menuAddonId,String name,BigDecimal price) {}
    public record Line(UUID id,UUID menuItemId,String itemName,String imageUrl,UUID variantId,String variantName,int quantity,BigDecimal baseUnitPrice,BigDecimal unitPrice,BigDecimal subtotal,String notes,List<Addon> addons) {}
    public record History(FoodOrderStatus oldStatus,FoodOrderStatus newStatus,String reason,Instant changedAt) {}
    public record Order(UUID id,long version,String orderNumber,UUID restaurantId,UUID reservationId,UUID groupBookingId,UUID corporateBookingId,UUID takeawayOrderId,UUID diniTicketId,OrderType orderType,FoodOrderStatus status,BigDecimal subtotal,BigDecimal discount,BigDecimal taxPercent,BigDecimal tax,BigDecimal total,String currency,String specialInstructions,String customerName,String customerPhone,List<Line> items,List<History> history,Instant createdAt) {}
    public record Tracking(boolean consent,BigDecimal customerLatitude,BigDecimal customerLongitude,BigDecimal restaurantLatitude,BigDecimal restaurantLongitude,Instant estimatedArrival,BigDecimal distanceKm,Instant lastLocationUpdatedAt,String estimationMethod) {}
    public record Takeaway(UUID id,long version,String orderNumber,Instant pickupTime,TakeawayStatus status,OrderPaymentStatus paymentStatus,String pickupCode,Instant collectedAt,Order order,Tracking tracking) {}
    public record Share(UUID customerId,BigDecimal amount) {}
    public record Combined(UUID bookingId,List<Order> orders,BigDecimal total,String currency,List<Share> proposedShares,String paymentMode) {}
}
