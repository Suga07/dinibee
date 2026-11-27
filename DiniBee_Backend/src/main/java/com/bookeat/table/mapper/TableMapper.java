package com.bookeat.table.mapper;
import com.bookeat.table.entity.RestaurantTable;
import com.bookeat.table.dto.TableResponse;
import org.springframework.stereotype.Component;
@Component
public class TableMapper {
    public TableResponse map(RestaurantTable t){return new TableResponse(t.getId(),t.getVersion(),t.getRestaurant().getId(),t.getTableNumber(),t.getTableName(),t.getCapacity(),t.getMinimumGuests(),t.getMaximumGuests(),t.getFloor(),t.getSection(),t.getShape(),t.getStatus(),t.getXPosition(),t.getYPosition(),t.getWidth(),t.getHeight(),t.getRotation(),t.isActive(),t.getCreatedAt(),t.getUpdatedAt());}
}
