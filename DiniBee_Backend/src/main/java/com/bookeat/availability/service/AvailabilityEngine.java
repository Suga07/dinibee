package com.bookeat.availability.service;
import com.bookeat.availability.enums.AvailabilityReason;
import com.bookeat.table.enums.TableStatus;
import com.bookeat.table.service.TableRules;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
/** Pure availability calculation. No writes, network calls, synthetic bookings or holds. */
public final class AvailabilityEngine {
    public record Settings(int minimumGuests,int maximumGuests,int slotMinutes,int durationMinutes,int graceMinutes,int advanceDays,int minimumAdvanceMinutes) {
        public Settings {if(minimumGuests<1 || maximumGuests<minimumGuests || slotMinutes<1 || durationMinutes<slotMinutes || graceMinutes<0 || advanceDays<1 || minimumAdvanceMinutes<0)throw new IllegalArgumentException("Invalid reservation settings");}
    }
    public record Schedule(LocalDate date,boolean closed,LocalTime opening,LocalTime closing,boolean nextDay,boolean special) {}
    public record Table(UUID id,int capacity,int minimumGuests,int maximumGuests,boolean active,TableStatus status) {}
    public record Occupancy(UUID tableId,Instant startsAt,Instant endsAt) {}
    public record Slot(OffsetDateTime startsAt,OffsetDateTime diningEndsAt,OffsetDateTime occupiedUntil,List<UUID> tableIds) {}
    public record Result(boolean available,AvailabilityReason reason,List<Slot> slots,List<UUID> requestedTableIds) {}
    private record Window(Instant start,Instant end) {}
    private AvailabilityEngine() {}
    public static Result calculate(ZoneId zone,LocalDate date,LocalTime requestedTime,ZoneOffset requestedOffset,int guests,Instant now,Settings settings,List<Schedule> schedules,List<Table> tables,List<Occupancy> occupied) {
        var today=now.atZone(zone).toLocalDate();
        if(date.isBefore(today) || date.isAfter(today.plusDays(settings.advanceDays())))return empty(AvailabilityReason.OUTSIDE_BOOKING_WINDOW);
        if(guests<settings.minimumGuests() || guests>settings.maximumGuests())return empty(AvailabilityReason.INVALID_GUEST_COUNT);
        var windows=windows(zone,date,schedules);if(windows.isEmpty())return empty(AvailabilityReason.CLOSED);
        var suitable=tables.stream().filter(t->TableRules.bookable(t.active(),t.status(),t.minimumGuests(),t.maximumGuests(),t.capacity(),guests)).toList();
        if(suitable.isEmpty())return empty(AvailabilityReason.NO_TABLE_CAPACITY);
        var indexes=occupied.stream().collect(Collectors.groupingBy(Occupancy::tableId)).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,e->new IntervalIndex(e.getValue().stream().map(o->new IntervalIndex.Range(o.startsAt(),o.endsAt())).toList())));
        var earliest=now.plusSeconds(settings.minimumAdvanceMinutes()*60L);var slots=new TreeMap<Instant,Slot>();
        for(var window:windows) {
            // Elapsed-time grid preserves real durations through DST gaps and repeated local clock times.
            for(Instant start=window.start();start.isBefore(window.end());start=start.plusSeconds(settings.slotMinutes()*60L)) {
                if(!start.atZone(zone).toLocalDate().equals(date) || start.isBefore(earliest))continue;
                Instant diningEnd=start.plusSeconds(settings.durationMinutes()*60L),end=diningEnd.plusSeconds(settings.graceMinutes()*60L);
                if(end.isAfter(window.end()))continue;
                Instant candidate=start;
                var ids=suitable.stream().filter(t->!indexes.containsKey(t.id()) || !indexes.get(t.id()).overlaps(candidate,end)).map(Table::id).toList();
                if(!ids.isEmpty())slots.put(start,new Slot(start.atZone(zone).toOffsetDateTime(),diningEnd.atZone(zone).toOffsetDateTime(),end.atZone(zone).toOffsetDateTime(),ids));
            }
        }
        var all=List.copyOf(slots.values());
        if(requestedTime==null)return new Result(!all.isEmpty(),all.isEmpty()?AvailabilityReason.NO_AVAILABLE_SLOT:AvailabilityReason.AVAILABLE,all,List.of());
        // Ambiguity is based on the timezone itself, not on which of the repeated slots happens to be free.
        if(requestedOffset==null && zone.getRules().getValidOffsets(date.atTime(requestedTime)).size()>1)
            return new Result(false,AvailabilityReason.AMBIGUOUS_LOCAL_TIME,all,List.of());
        var match=all.stream().filter(s->s.startsAt().toLocalTime().equals(requestedTime) && (requestedOffset==null || s.startsAt().getOffset().equals(requestedOffset))).findFirst();
        return match.map(s->new Result(true,AvailabilityReason.AVAILABLE,all,s.tableIds())).orElseGet(()->new Result(false,AvailabilityReason.NO_AVAILABLE_SLOT,all,List.of()));
    }
    public static boolean containsWindow(ZoneId zone,LocalDate date,Instant start,Instant end,List<Schedule> schedules) {
        return start.isBefore(end) && windows(zone,date,schedules).stream().anyMatch(w->!start.isBefore(w.start())&&!end.isAfter(w.end()));
    }
    private static Result empty(AvailabilityReason reason){return new Result(false,reason,List.of(),List.of());}
    private static List<Window> windows(ZoneId zone,LocalDate date,List<Schedule> schedules) {
        var byDate=schedules.stream().collect(Collectors.toMap(Schedule::date,s->s));var result=new ArrayList<Window>();var current=byDate.get(date);
        if(current!=null && current.special() && current.closed())return result;
        for(var d:List.of(date.minusDays(1),date)) {
            var schedule=byDate.get(d);
            if(schedule==null || schedule.closed() || schedule.opening()==null || schedule.closing()==null)continue;
            if(d.isBefore(date) && (current!=null && current.special()))continue; // A dated exception replaces carry-in too.
            var localEnd=d.plusDays(schedule.nextDay()?1:0).atTime(schedule.closing());
            Instant start=d.atTime(schedule.opening()).atZone(zone).toInstant();
            Instant end=localEnd.atZone(zone).withLaterOffsetAtOverlap().toInstant();
            var next=byDate.get(d.plusDays(1));
            if(schedule.nextDay() && next!=null && next.special()) {
                var midnight=d.plusDays(1).atStartOfDay(zone).toInstant();if(end.isAfter(midnight))end=midnight;
            }
            if(start.isBefore(end) && end.isAfter(date.atStartOfDay(zone).toInstant()))result.add(new Window(start,end));
        }
        return result;
    }
}
