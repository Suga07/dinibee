package com.bookeat.restaurant.service;
import com.bookeat.restaurant.entity.Restaurant;
import com.bookeat.restaurant.enums.ApprovalStatus;
import com.bookeat.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
@Service @RequiredArgsConstructor
public class RestaurantInventoryAccess {
    private final RestaurantAccessService access;
    @Transactional(propagation=Propagation.MANDATORY)
    public Restaurant lockEditable(UUID actor,UUID restaurantId,long version) {
        var r=access.lockOwnedRestaurant(actor,restaurantId,version);
        if(!WorkflowRules.ownerEditable(r.getApprovalStatus()) && r.getApprovalStatus()!=ApprovalStatus.APPROVED)
            throw new ApiException(ErrorCode.RESTAURANT_STATE_INVALID);
        return r;
    }
}
