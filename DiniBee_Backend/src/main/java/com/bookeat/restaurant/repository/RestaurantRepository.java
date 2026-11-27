package com.bookeat.restaurant.repository;
import com.bookeat.restaurant.entity.Restaurant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
public interface RestaurantRepository extends JpaRepository<Restaurant,UUID>, JpaSpecificationExecutor<Restaurant> {
    Optional<Restaurant> findByIdAndOwnerUserId(UUID id, UUID userId);
    Page<Restaurant> findByOwnerUserId(UUID userId, Pageable pageable);
    Optional<Restaurant> findFirstByOwnerUserIdOrderByCreatedAtAscIdAsc(UUID userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from Restaurant r where r.id=:id and r.owner.user.id=:userId")
    Optional<Restaurant> lockOwned(@Param("id") UUID id, @Param("userId") UUID userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from Restaurant r where r.id=:id")
    Optional<Restaurant> lockById(@Param("id") UUID id);
    @Query(value="select r from Restaurant r join fetch r.owner o join fetch o.user u",
           countQuery="select count(r) from Restaurant r")
    Page<Restaurant> findAllWithOwner(Pageable pageable);

    @Query("select r from Restaurant r join fetch r.owner o join fetch o.user u where r.phone in :phones or r.alternatePhone in :phones")
    List<Restaurant> findByPhoneOrAlternatePhoneIn(@Param("phones") List<String> phones);
}

