package com.bookeat.payment.repository;
import com.bookeat.payment.entity.Payment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;
public interface PaymentRepository extends JpaRepository<Payment,UUID> {
Page<Payment> findByCustomerId(UUID id,Pageable p);
Page<Payment> findByRestaurantId(UUID id,Pageable p);
Optional<Payment> findByGatewayOrderId(String id);
Optional<Payment> findByGatewayPaymentId(String id);
Optional<Payment> findFirstByCustomerIdAndReferenceTypeAndReferenceIdAndStatusInOrderByCreatedAtDesc(UUID customerId,com.bookeat.payment.enums.PaymentReferenceType referenceType,UUID referenceId,Collection<com.bookeat.payment.enums.PaymentStatus> statuses);
List<Payment> findByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(com.bookeat.payment.enums.PaymentReferenceType referenceType,UUID referenceId);
@Query("select p.restaurantId from Payment p where p.id=:id") Optional<UUID> restaurantId(@Param("id") UUID id);
}
