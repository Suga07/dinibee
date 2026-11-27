package com.bookeat.owner.repository;
import com.bookeat.owner.entity.RestaurantOwner;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface RestaurantOwnerRepository extends JpaRepository<RestaurantOwner, UUID> {
    Optional<RestaurantOwner> findByUserId(UUID userId);
}
