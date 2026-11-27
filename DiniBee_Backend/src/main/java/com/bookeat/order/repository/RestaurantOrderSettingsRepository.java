package com.bookeat.order.repository;
import com.bookeat.order.entity.RestaurantOrderSettings;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;
public interface RestaurantOrderSettingsRepository extends JpaRepository<RestaurantOrderSettings,UUID> {
    Optional<RestaurantOrderSettings> findByRestaurantId(UUID id);
}
