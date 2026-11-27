package com.bookeat.reservation.service;
import com.bookeat.reservation.enums.*;
import java.time.*;
import java.util.*;
public final class BookingRules {
    private BookingRules() {}
    public static boolean editable(ReservationStatus s){return s==ReservationStatus.PENDING||s==ReservationStatus.CONFIRMED;}
    public static boolean ownerTransition(ReservationStatus from,ReservationStatus to){return switch(to){case CONFIRMED->from==ReservationStatus.PENDING;case REJECTED->from==ReservationStatus.PENDING;case NO_SHOW->editable(from);default->false;};}
    public static boolean cancellable(ReservationStatus s,boolean allowed,Instant deadline,Instant now){return editable(s)&&allowed&&now.isBefore(deadline);}
    public static List<OffsetDateTime> occurrences(OffsetDateTime first,ZoneId zone,RecurrenceType type,int count,int interval){if(count<1||count>12||interval<1||interval>12||(type==RecurrenceType.NONE&&count!=1))throw new IllegalArgumentException("Invalid recurrence");var result=new ArrayList<OffsetDateTime>();var local=first.atZoneSameInstant(zone);for(int i=0;i<count;i++){int n=i*interval;var next=switch(type){case NONE->local;case DAILY->local.plusDays(n);case WEEKLY->local.plusWeeks(n);case MONTHLY->local.plusMonths(n);};result.add(next.toOffsetDateTime());}return List.copyOf(result);}
}
