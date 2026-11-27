package com.bookeat.diniticket.service;
import com.bookeat.diniticket.enums.TicketStatus;
import java.util.Set;
public final class TicketRules {
    private TicketRules() {}
    public static final Set<TicketStatus> ACTIVE=Set.of(TicketStatus.WAITING,TicketStatus.CALLED,TicketStatus.ARRIVED,TicketStatus.SEATED);
    public static final Set<TicketStatus> QUEUED=Set.of(TicketStatus.WAITING,TicketStatus.CALLED,TicketStatus.ARRIVED);
    public static boolean transition(TicketStatus from,TicketStatus to){return switch(to){case CALLED->from==TicketStatus.WAITING;case ARRIVED->from==TicketStatus.CALLED;case SEATED->from==TicketStatus.ARRIVED;case COMPLETED->from==TicketStatus.SEATED;case CANCELLED->QUEUED.contains(from);case EXPIRED->from==TicketStatus.WAITING||from==TicketStatus.CALLED;case NO_SHOW->from==TicketStatus.WAITING||from==TicketStatus.CALLED;default->false;};}
    public static int estimatedWait(int position,int minutesPerParty){if(position<0||minutesPerParty<0)throw new IllegalArgumentException("Invalid queue estimate");return Math.multiplyExact(Math.max(0,position-1),minutesPerParty);}
}
