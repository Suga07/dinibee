package com.bookeat.table.repository;
import com.bookeat.table.entity.RestaurantTable;
import com.bookeat.table.enums.TableStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable,UUID> {
    Optional<RestaurantTable> findByIdAndRestaurantIdAndDeletedAtIsNull(UUID id,UUID restaurantId);
    Page<RestaurantTable> findByRestaurantIdAndDeletedAtIsNull(UUID restaurantId,Pageable page);
    List<RestaurantTable> findByRestaurantIdAndDeletedAtIsNullOrderByCapacityAscTableNumberAsc(UUID restaurantId);
    long countByRestaurantIdAndDeletedAtIsNull(UUID restaurantId);
    long countByRestaurantIdAndDeletedAtIsNullAndStatus(UUID restaurantId, TableStatus status);
    boolean existsByRestaurantIdAndTableNumberAndDeletedAtIsNullAndIdNot(UUID restaurantId,String tableNumber,UUID id);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select t from RestaurantTable t where t.id=:id and t.restaurant.id=:restaurantId and t.deletedAt is null")
    Optional<RestaurantTable> lockOwned(@Param("id") UUID id,@Param("restaurantId") UUID restaurantId);
}
