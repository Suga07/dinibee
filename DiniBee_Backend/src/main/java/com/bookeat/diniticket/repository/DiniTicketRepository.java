package com.bookeat.diniticket.repository;
import com.bookeat.diniticket.entity.DiniTicket;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;
public interface DiniTicketRepository extends JpaRepository<DiniTicket,UUID> {
    @Query("select distinct t.restaurantId from DiniTicket t where t.customerId=:customer and t.status in :statuses order by t.restaurantId") List<UUID> activeRestaurantIds(@Param("customer") UUID customer,@Param("statuses") Collection<com.bookeat.diniticket.enums.TicketStatus> statuses);
    Page<DiniTicket> findByRestaurantId(UUID id,Pageable page);
    Page<DiniTicket> findByCustomerId(UUID id,Pageable page);
    List<DiniTicket> findByRestaurantIdAndStatusInOrderByQueueSequenceAsc(UUID id,Collection<com.bookeat.diniticket.enums.TicketStatus> statuses);
    List<DiniTicket> findByCustomerIdAndStatusInOrderByQueueSequenceAsc(UUID id,Collection<com.bookeat.diniticket.enums.TicketStatus> statuses);
    boolean existsByRestaurantIdAndCustomerIdAndStatusIn(UUID rid,UUID cid,Collection<com.bookeat.diniticket.enums.TicketStatus> statuses);
    @Query("select t.restaurantId from DiniTicket t where t.id=:id") Optional<UUID> restaurantId(@Param("id") UUID id);
    /**
     * Returns up to `page.pageSize` restaurant IDs that have DiniTickets ready
     * to expire.  Uses LEFT JOIN instead of NOT EXISTS to avoid the correlated
     * sub-query that was exhausting PostgreSQL MessageContext memory on low-RAM
     * machines.  ORDER BY is omitted intentionally – ordering is not required
     * for correctness and was the largest contributor to the sort-buffer OOM.
     */
    @Query(value="select t.restaurant_id from dini_tickets t " +
            "join diniticket_settings s on s.restaurant_id=t.restaurant_id " +
            "left join food_orders o on o.dini_ticket_id=t.id " +
            "    and o.status not in ('PLACED','COMPLETED','CANCELLED','REJECTED') " +
            "where s.auto_expire_tickets " +
            "  and t.status in ('WAITING','CALLED') " +
            "  and t.expires_at<=:now " +
            "  and o.id is null " +
            "group by t.restaurant_id",
            nativeQuery=true)
    List<UUID> expiringRestaurants(@Param("now") Instant now, Pageable page);
}
