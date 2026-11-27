package com.bookeat.availability.repository;
import com.bookeat.availability.entity.TableAllocation;
import com.bookeat.availability.enums.AllocationType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;
public interface TableAllocationRepository extends JpaRepository<TableAllocation,UUID> {
    @Query("select a from TableAllocation a where a.restaurant.id=:restaurantId and a.active=true and a.startsAt < :endsAt and a.endsAt > :startsAt order by a.startsAt")
    List<TableAllocation> overlapping(@Param("restaurantId") UUID restaurantId,@Param("startsAt") Instant startsAt,@Param("endsAt") Instant endsAt);
    boolean existsByTableIdAndActiveTrueAndEndsAtAfter(UUID tableId,Instant now);
    Optional<TableAllocation> findByRestaurantIdAndTableIdAndReferenceTypeAndReferenceIdAndActiveTrue(UUID restaurantId,UUID tableId,AllocationType type,UUID referenceId);
    Optional<TableAllocation> findByIdAndRestaurantIdAndReferenceId(UUID id,UUID restaurantId,UUID referenceId);
}
