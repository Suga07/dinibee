package com.bookeat.payment.repository;
import com.bookeat.payment.entity.Refund;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;
public interface RefundRepository extends JpaRepository<Refund,UUID> {
List<Refund> findByPaymentId(UUID id);
Optional<Refund> findByGatewayRefundId(String id);
}
