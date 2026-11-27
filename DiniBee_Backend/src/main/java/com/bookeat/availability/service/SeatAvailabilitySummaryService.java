package com.bookeat.availability.service;
import com.bookeat.availability.dto.SeatAvailabilitySummary;import lombok.RequiredArgsConstructor;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.stereotype.Service;import java.time.*;import java.util.*;
@Service @RequiredArgsConstructor
public class SeatAvailabilitySummaryService {
 private final JdbcTemplate jdbc;
 public SeatAvailabilitySummary find(UUID restaurantId, LocalDate date){
  var rows=jdbc.query("""
    select t.capacity,
      count(*) as total_tables,
      count(*) filter (where t.status in ('BLOCKED','MAINTENANCE') or exists (
        select 1 from reservations r where r.restaurant_id=t.restaurant_id and r.table_id=t.id and r.reservation_date=?
        and r.status in ('PENDING','CONFIRMED','ARRIVED','CHECKED_IN','DINING')
      )) as booked_tables
    from restaurant_tables t
    where t.restaurant_id=? and t.active=true and t.deleted_at is null
    group by t.capacity order by t.capacity
    """,(rs,n)->new int[]{rs.getInt("capacity"),rs.getInt("total_tables"),rs.getInt("booked_tables")},date,restaurantId);
  List<SeatAvailabilitySummary.Category> categories=new ArrayList<>();int total=0,booked=0;
  for(var r:rows){int capacity=r[0],tt=r[1],bt=r[2],at=Math.max(0,tt-bt);int ts=capacity*tt,bs=capacity*bt,as=capacity*at;total+=ts;booked+=bs;categories.add(new SeatAvailabilitySummary.Category(capacity,tt,bt,at,ts,bs,as));}
  return new SeatAvailabilitySummary(restaurantId,date,total,booked,Math.max(0,total-booked),categories,Instant.now());
 }
}
