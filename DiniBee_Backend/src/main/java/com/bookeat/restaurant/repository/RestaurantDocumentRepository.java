package com.bookeat.restaurant.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bookeat.restaurant.entity.RestaurantDocument;
public interface RestaurantDocumentRepository extends JpaRepository<RestaurantDocument, UUID> {
    List<RestaurantDocument> findByRestaurantIdOrderByCreatedAtAscIdAsc(UUID restaurantId);
    List<RestaurantDocument> findByRestaurantIdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(UUID restaurantId);
    Optional<RestaurantDocument> findByIdAndRestaurantIdAndDeletedAtIsNull(UUID id, UUID restaurantId);
}
