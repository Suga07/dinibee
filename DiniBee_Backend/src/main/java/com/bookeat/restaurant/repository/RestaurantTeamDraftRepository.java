package com.bookeat.restaurant.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bookeat.restaurant.entity.RestaurantTeamDraft;
public interface RestaurantTeamDraftRepository extends JpaRepository<RestaurantTeamDraft, UUID> {
    List<RestaurantTeamDraft> findByRestaurantIdOrderByCreatedAtAscIdAsc(UUID restaurantId);
    void deleteByRestaurantId(UUID restaurantId);
}
