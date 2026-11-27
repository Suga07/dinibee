package com.bookeat.order.service;
import java.util.UUID;
/** Phase 8 implementation joins the order's restaurant-locked transaction. */
public interface TicketOrderAuthorizer { void authorize(UUID customerId,UUID restaurantId,UUID ticketId); void requireSeated(UUID ticketId); }
