package com.bookeat.diniticket.service;
import com.bookeat.diniticket.entity.DiniTicket;import java.util.UUID;
public interface TicketSeatingObserver {void seated(DiniTicket ticket,UUID actor);}
