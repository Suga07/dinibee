package com.bookeat.diniticket.repository;
import com.bookeat.diniticket.entity.DiniTicket;
import com.bookeat.diniticket.enums.TicketStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.*;
import java.time.Instant;
import java.util.*;
@Repository @RequiredArgsConstructor
public class QueueEventRepository {
    private final JdbcTemplate jdbc;
    @Transactional(propagation=Propagation.MANDATORY) public void append(DiniTicket t,String type){jdbc.update("insert into queue_events(restaurant_id,customer_id,ticket_id,event_type,status,queue_position,estimated_wait_minutes) values (?,?,?,?,?,?,?)",t.getRestaurantId(),t.getCustomerId(),t.getId(),type,t.getStatus().name(),t.getQueuePosition(),t.getEstimatedWaitMinutes());}
    public List<Event> customer(UUID customer,long after){return jdbc.query("select * from queue_events where customer_id=? and id>? order by id limit 100",(rs,n)->new Event(rs.getLong("id"),rs.getObject("restaurant_id",UUID.class),rs.getObject("ticket_id",UUID.class),rs.getString("event_type"),TicketStatus.valueOf(rs.getString("status")),rs.getInt("queue_position"),rs.getInt("estimated_wait_minutes"),rs.getTimestamp("created_at").toInstant()),customer,after);}
    public List<Event> owner(UUID restaurant,long after){return jdbc.query("select * from queue_events where restaurant_id=? and id>? order by id limit 100",(rs,n)->new Event(rs.getLong("id"),rs.getObject("restaurant_id",UUID.class),rs.getObject("ticket_id",UUID.class),rs.getString("event_type"),TicketStatus.valueOf(rs.getString("status")),rs.getInt("queue_position"),rs.getInt("estimated_wait_minutes"),rs.getTimestamp("created_at").toInstant()),restaurant,after);}
    public record Event(long id,UUID restaurantId,UUID ticketId,String type,TicketStatus status,int queuePosition,int estimatedWaitMinutes,Instant createdAt) {}
}
