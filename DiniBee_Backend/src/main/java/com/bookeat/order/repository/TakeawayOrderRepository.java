package com.bookeat.order.repository;
import com.bookeat.order.entity.TakeawayOrder;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;
public interface TakeawayOrderRepository extends JpaRepository<TakeawayOrder,UUID> {
    Page<TakeawayOrder> findByCustomerId(UUID id,Pageable page);
    Page<TakeawayOrder> findByRestaurantId(UUID id,Pageable page);
    @Query("select t from TakeawayOrder t where t.restaurantId=:id and " +
           "(t.paymentStatus = com.bookeat.order.enums.OrderPaymentStatus.PAID or " +
           "t.status <> com.bookeat.order.enums.TakeawayStatus.PLACED or " +
           "t.total <= 0 or " +
           "exists (select 1 from com.bookeat.payment.entity.Payment p where p.referenceId = t.id and p.referenceType = com.bookeat.payment.enums.PaymentReferenceType.TAKEAWAY and p.gateway = com.bookeat.payment.enums.PaymentGateway.CASH and p.status <> com.bookeat.payment.enums.PaymentStatus.FAILED))")
    Page<TakeawayOrder> findConfirmedByRestaurantId(@Param("id") UUID id,Pageable page);
    boolean existsByOrderNumber(String orderNumber);
    @Query("select t.restaurantId from TakeawayOrder t where t.id=:id") Optional<UUID> restaurantId(@Param("id") UUID id);
}
