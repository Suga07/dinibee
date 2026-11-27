package com.bookeat.order.service;
import java.util.UUID;import com.bookeat.order.entity.FoodOrder;import com.bookeat.order.enums.FoodOrderStatus;
public interface OrderLifecycleGuard {default void beforeCreate(UUID reservation,UUID ticket){} default void beforeTransition(FoodOrder order,FoodOrderStatus target){} }
