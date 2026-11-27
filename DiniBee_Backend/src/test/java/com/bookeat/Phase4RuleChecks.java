package com.bookeat;
import com.bookeat.availability.service.*;
import com.bookeat.availability.service.AvailabilityEngine.*;
import com.bookeat.availability.enums.AvailabilityReason;
import com.bookeat.table.enums.TableStatus;
import com.bookeat.table.service.TableRules;
import com.bookeat.menu.service.MenuPricing;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
/** Dependency-free executable checks against the production rules; also invoked by JUnit. */
public final class Phase4RuleChecks {
    static int assertions;
    static final UUID TABLE=UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final LocalDate DATE=LocalDate.of(2026,6,10);
    static final Settings CONFIG=new Settings(1,8,30,60,15,30,30);
    static final List<Table> TABLES=List.of(new Table(TABLE,4,1,4,true,TableStatus.AVAILABLE));
    static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    static Schedule hours(LocalDate day,String opening,String closing,boolean overnight,boolean special){return new Schedule(day,false,LocalTime.parse(opening),LocalTime.parse(closing),overnight,special);}
    static Result normal(String time,int guests,Instant now,List<Occupancy> occupancy){return AvailabilityEngine.calculate(ZoneOffset.UTC,DATE,time==null?null:LocalTime.parse(time),null,guests,now,CONFIG,List.of(hours(DATE,"09:00","22:00",false,false)),TABLES,occupancy);}
    static Instant instant(String time){return DATE.atTime(LocalTime.parse(time)).toInstant(ZoneOffset.UTC);}
    public static void tableRules() {
        check(TableRules.normalizeNumber(" t-01 ").equals("T-01"),"normalized number");
        check(TableRules.validCapacity(4,2,4),"valid capacity");
        for(int[] x:new int[][]{{0,1,1},{501,1,2},{4,0,4},{4,3,2},{4,1,5}})check(!TableRules.validCapacity(x[0],x[1],x[2]),"invalid capacity");
        for(var status:TableStatus.values())check(TableRules.bookable(true,status,2,4,4,3)==(status==TableStatus.AVAILABLE),"status "+status);
        check(!TableRules.bookable(false,TableStatus.AVAILABLE,1,4,4,2),"inactive");
        check(!TableRules.bookable(true,TableStatus.AVAILABLE,2,4,4,1),"below minimum");
        check(!TableRules.bookable(true,TableStatus.AVAILABLE,1,4,4,5),"above maximum");
    }
    public static void pricing() {
        var base=new BigDecimal("100.00");var discount=new BigDecimal("80.00");
        check(MenuPricing.sellingPrice(base,null).equals(base),"base price");
        check(MenuPricing.sellingPrice(base,discount).equals(discount),"discount price");
        check(MenuPricing.unitPrice(base,discount,null,List.of(new BigDecimal("7.50"))).equals(new BigDecimal("87.50")),"addon sum");
        check(MenuPricing.unitPrice(base,discount,new BigDecimal("150"),List.of(new BigDecimal("7.50"),new BigDecimal("2.50"))).equals(new BigDecimal("160.00")),"variant replaces discounted base");
        check(!MenuPricing.validDiscount(base,new BigDecimal("101")),"discount above base");
        check(!MenuPricing.validDiscount(base,new BigDecimal("-1")),"negative discount");
        check(!MenuPricing.validDiscount(null,null),"missing base");
        check(MenuPricing.sellingPrice(BigDecimal.ZERO,BigDecimal.ZERO).equals(new BigDecimal("0.00")),"free item");
        boolean rejected=false;try{MenuPricing.unitPrice(base,null,null,List.of(new BigDecimal("-1")));}catch(IllegalArgumentException ex){rejected=true;}check(rejected,"negative addon");
        rejected=false;try{MenuPricing.sellingPrice(new BigDecimal("1.001"),null);}catch(ArithmeticException ex){rejected=true;}check(rejected,"no silent price rounding");
    }
    public static void intervalIndex() {
        var ranges=List.of(new IntervalIndex.Range(instant("10:00"),instant("11:00")),new IntervalIndex.Range(instant("11:00"),instant("12:00")));
        var index=new IntervalIndex(ranges);
        check(!index.overlaps(instant("09:00"),instant("10:00")),"left adjacency");
        check(!index.overlaps(instant("12:00"),instant("13:00")),"right adjacency");
        check(index.overlaps(instant("10:30"),instant("11:30")),"merged overlap");
        var random=new Random(4917);var origin=Instant.EPOCH;
        for(int fixture=0;fixture<100;fixture++) {
            var source=new ArrayList<IntervalIndex.Range>();
            for(int i=0;i<random.nextInt(50);i++){int start=random.nextInt(10000);source.add(new IntervalIndex.Range(origin.plusSeconds(start),origin.plusSeconds(start+1+random.nextInt(400))));}
            var actual=new IntervalIndex(source);
            for(int i=0;i<50;i++){var start=origin.plusSeconds(random.nextInt(10500));var end=start.plusSeconds(1+random.nextInt(500));boolean expected=source.stream().anyMatch(r->r.start().isBefore(end)&&r.end().isAfter(start));check(actual.overlaps(start,end)==expected,"interval oracle fixture "+fixture);}
        }
    }
    public static void bookingWindowAndCapacity() {
        var now=instant("08:00");check(normal("09:00",2,now,List.of()).available(),"opening slot");
        check(!normal("21:00",2,now,List.of()).available(),"grace must fit before close");
        check(normal("20:30",2,now,List.of()).available(),"last fitting slot");
        check(!normal("09:15",2,now,List.of()).available(),"not on slot grid");
        check(!normal("09:00",2,instant("08:31"),List.of()).available(),"minimum advance");
        check(normal("09:00",2,instant("08:30"),List.of()).available(),"minimum advance inclusive");
        check(normal("12:00",5,now,List.of()).reason()==AvailabilityReason.NO_TABLE_CAPACITY,"capacity cannot be combined");
        check(normal("12:00",9,now,List.of()).reason()==AvailabilityReason.INVALID_GUEST_COUNT,"restaurant guest maximum");
        var day=normal(null,2,now,List.of());check(day.available()&&day.requestedTableIds().isEmpty(),"date only lists slots without selecting tables");
        check(day.slots().get(0).startsAt().toLocalTime().equals(LocalTime.of(9,0)),"sorted slots");
        check(normal("12:00",2,DATE.minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC),List.of()).available(),"horizon inclusive");
        check(normal("12:00",2,DATE.minusDays(31).atStartOfDay().toInstant(ZoneOffset.UTC),List.of()).reason()==AvailabilityReason.OUTSIDE_BOOKING_WINDOW,"horizon exceeded");
        check(normal("12:00",2,DATE.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC),List.of()).reason()==AvailabilityReason.OUTSIDE_BOOKING_WINDOW,"past day");
    }
    public static void allocationsAndGrace() {
        var occupied=List.of(new Occupancy(TABLE,instant("10:00"),instant("11:15")));
        check(!normal("09:00",2,instant("08:00"),occupied).available(),"candidate grace overlaps next booking");
        check(!normal("11:00",2,instant("08:00"),occupied).available(),"previous grace blocks booking");
        check(normal("11:30",2,instant("08:00"),occupied).available(),"after grace");
        check(normal("09:00",2,instant("08:00"),List.of(new Occupancy(TABLE,instant("10:15"),instant("11:00")))).available(),"exact adjacency");
        check(normal("09:00",2,instant("08:00"),List.of(new Occupancy(UUID.randomUUID(),instant("09:00"),instant("12:00")))).available(),"other table not blocked");
        var first=normal("09:00",2,instant("08:00"),List.of());check(first.equals(normal("09:00",2,instant("08:00"),List.of())),"repeat read creates no hold");
    }
    public static void overnightAndExceptions() {
        var schedules=new ArrayList<Schedule>();schedules.add(hours(DATE.minusDays(1),"20:00","02:00",true,false));schedules.add(new Schedule(DATE,true,null,null,false,false));
        var now=instant("00:00");
        var carry=AvailabilityEngine.calculate(ZoneOffset.UTC,DATE,LocalTime.of(0,30),null,2,now,CONFIG,schedules,TABLES,List.of());check(carry.available(),"regular closed day permits previous overnight carry-in");
        schedules.set(1,new Schedule(DATE,true,null,null,false,true));
        check(AvailabilityEngine.calculate(ZoneOffset.UTC,DATE,LocalTime.of(0,30),null,2,now,CONFIG,schedules,TABLES,List.of()).reason()==AvailabilityReason.CLOSED,"dated closure overrides overnight");
        schedules.set(1,hours(DATE,"12:00","15:00",false,true));
        check(!AvailabilityEngine.calculate(ZoneOffset.UTC,DATE,LocalTime.of(0,30),null,2,now,CONFIG,schedules,TABLES,List.of()).available(),"dated hours replace carry-in");
        check(AvailabilityEngine.calculate(ZoneOffset.UTC,DATE,LocalTime.of(12,0),null,2,now,CONFIG,schedules,TABLES,List.of()).available(),"dated hours open");
        var nextClosed=List.of(hours(DATE,"20:00","02:00",true,false),new Schedule(DATE.plusDays(1),true,null,null,false,true));
        check(!AvailabilityEngine.calculate(ZoneOffset.UTC,DATE,LocalTime.of(23,0),null,2,now,CONFIG,nextClosed,TABLES,List.of()).available(),"next day closure clips interval at midnight");
        check(AvailabilityEngine.calculate(ZoneOffset.UTC,DATE,LocalTime.of(22,30),null,2,now,CONFIG,nextClosed,TABLES,List.of()).available(),"before next day closure");
    }
    public static void daylightSaving() {
        var zone=ZoneId.of("America/New_York");var cfg=new Settings(1,8,30,60,0,30,0);
        var fall=LocalDate.of(2026,11,1);var fallNow=fall.atStartOfDay(zone).toInstant();var fallHours=List.of(hours(fall,"00:00","04:00",false,false));
        var ambiguous=AvailabilityEngine.calculate(zone,fall,LocalTime.of(1,30),null,2,fallNow,cfg,fallHours,TABLES,List.of());
        check(ambiguous.reason()==AvailabilityReason.AMBIGUOUS_LOCAL_TIME,"repeated time requires offset");
        var early=AvailabilityEngine.calculate(zone,fall,LocalTime.of(1,30),ZoneOffset.of("-04:00"),2,fallNow,cfg,fallHours,TABLES,List.of());
        var late=AvailabilityEngine.calculate(zone,fall,LocalTime.of(1,30),ZoneOffset.of("-05:00"),2,fallNow,cfg,fallHours,TABLES,List.of());
        check(early.available()&&late.available(),"both explicit offsets selectable");
        check(ambiguous.slots().stream().filter(s->s.startsAt().toLocalTime().equals(LocalTime.of(1,30))).count()==2,"repeated slots have distinct instants");
        for(var slot:early.slots())check(Duration.between(slot.startsAt(),slot.diningEndsAt()).toMinutes()==60,"elapsed fall duration");
        var spring=LocalDate.of(2026,3,8);var springNow=spring.atStartOfDay(zone).toInstant();var springHours=List.of(hours(spring,"00:00","05:00",false,false));
        var gap=AvailabilityEngine.calculate(zone,spring,LocalTime.of(2,30),null,2,springNow,cfg,springHours,TABLES,List.of());
        check(!gap.available(),"nonexistent local time unavailable");check(gap.slots().stream().noneMatch(s->s.startsAt().getHour()==2),"no synthetic gap slots");
        for(var slot:gap.slots())check(Duration.between(slot.startsAt(),slot.diningEndsAt()).toMinutes()==60,"elapsed spring duration");
    }
    public static void main(String[] args){tableRules();pricing();intervalIndex();bookingWindowAndCapacity();allocationsAndGrace();overnightAndExceptions();daylightSaving();System.out.println("PASS: "+assertions+" Phase 4 executable rule assertions against production Java classes");}
}
