package com.bookeat.dining.service;
import java.util.UUID;
public interface DiningPaymentPort {boolean settled(UUID reservationId,UUID ticketId);}
