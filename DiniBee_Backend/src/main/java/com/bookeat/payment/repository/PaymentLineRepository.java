package com.bookeat.payment.repository;
import com.bookeat.payment.entity.PaymentLine;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;
public interface PaymentLineRepository extends JpaRepository<PaymentLine,UUID> {
List<PaymentLine> findByPaymentId(UUID id);
Optional<PaymentLine> findByFoodOrderId(UUID id);
}
