package com.bookeat.restaurant.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bookeat.restaurant.entity.DiniTicketSettings;
public interface DiniTicketSettingsRepository extends JpaRepository<DiniTicketSettings, UUID> {
    Optional<DiniTicketSettings> findByRestaurantId(UUID restaurantId);
}
