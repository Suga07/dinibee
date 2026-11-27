package com.bookeat.menu.service;
import com.bookeat.menu.dto.*;
import com.bookeat.menu.enums.*;
import com.bookeat.common.response.*;
import java.util.UUID;
public interface MenuService {
    RestaurantResource<MenuResponses.Category> createCategory(UUID actor,UUID restaurantId,MenuRequests.Category request);
    PageResponse<MenuResponses.Category> categories(UUID actor,UUID restaurantId,int page,int size);
    RestaurantResource<MenuResponses.Category> getCategory(UUID actor,UUID restaurantId,UUID categoryId);
    RestaurantResource<MenuResponses.Category> updateCategory(UUID actor,UUID restaurantId,UUID categoryId,MenuRequests.Category request);
    RestaurantResource<UUID> deleteCategory(UUID actor,UUID restaurantId,UUID categoryId,long restaurantVersion);
    RestaurantResource<MenuResponses.Item> createItem(UUID actor,UUID restaurantId,MenuRequests.Item request);
    PageResponse<MenuResponses.Item> items(UUID actor,UUID restaurantId,UUID categoryId,FoodType foodType,Boolean available,String search,int page,int size);
    RestaurantResource<MenuResponses.Item> getItem(UUID actor,UUID restaurantId,UUID itemId);
    RestaurantResource<MenuResponses.Item> updateItem(UUID actor,UUID restaurantId,UUID itemId,MenuRequests.Item request);
    RestaurantResource<UUID> deleteItem(UUID actor,UUID restaurantId,UUID itemId,long restaurantVersion);
    RestaurantResource<MenuResponses.Item> availability(UUID actor,UUID restaurantId,UUID itemId,MenuRequests.Availability request);
    String uploadItemImage(UUID actor,UUID restaurantId,org.springframework.web.multipart.MultipartFile file,String baseUri);
}
