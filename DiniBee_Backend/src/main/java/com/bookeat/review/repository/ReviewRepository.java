package com.bookeat.review.repository;
import com.bookeat.review.entity.Review;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;
public interface ReviewRepository extends JpaRepository<Review,UUID> {
Page<Review> findByCustomerIdAndStatusNot(UUID id,com.bookeat.review.enums.ReviewStatus status,Pageable p);
Page<Review> findByRestaurantIdAndStatus(UUID id,com.bookeat.review.enums.ReviewStatus status,Pageable p);
Optional<Review> findByCustomerIdAndReservationId(UUID customerId, UUID reservationId);
Optional<Review> findByCustomerIdAndFoodOrderId(UUID customerId, UUID foodOrderId);
}
