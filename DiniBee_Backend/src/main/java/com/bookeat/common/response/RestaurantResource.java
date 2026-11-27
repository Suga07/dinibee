package com.bookeat.common.response;
/** Parent version is separate from the returned resource's own optimistic version. */
public record RestaurantResource<T>(long restaurantVersion,T resource) {}
