package com.bookeat.commerce.service;
import com.bookeat.restaurant.entity.Restaurant;
import com.bookeat.restaurant.repository.*;
import com.bookeat.availability.service.AvailabilityEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
@Service @RequiredArgsConstructor
public class RestaurantWindowService {
    private final RestaurantOperatingHourRepository hours;private final RestaurantSpecialScheduleRepository specials;
    public boolean contains(Restaurant r,Instant start,Instant end){if(r.getTimezone()==null)return false;var zone=ZoneId.of(r.getTimezone());var date=start.atZone(zone).toLocalDate();var weekly=hours.findByRestaurantIdOrderByCreatedAtAscIdAsc(r.getId()).stream().collect(Collectors.toMap(h->h.getDayOfWeek(),h->h));var special=specials.findByRestaurantIdOrderByCreatedAtAscIdAsc(r.getId()).stream().collect(Collectors.toMap(s->s.getDate(),s->s));var schedules=new ArrayList<AvailabilityEngine.Schedule>();for(var d:List.of(date.minusDays(1),date,date.plusDays(1))){var s=special.get(d);var h=weekly.get(d.getDayOfWeek());if(s!=null)schedules.add(new AvailabilityEngine.Schedule(d,s.isClosed(),s.getOpeningTime(),s.getClosingTime(),s.isClosesNextDay(),true));else if(h!=null)schedules.add(new AvailabilityEngine.Schedule(d,h.isClosed(),h.getOpeningTime(),h.getClosingTime(),h.isClosesNextDay(),false));}return AvailabilityEngine.containsWindow(zone,date,start,end,schedules);}
}
