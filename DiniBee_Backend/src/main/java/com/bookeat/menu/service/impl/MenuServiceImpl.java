package com.bookeat.menu.service.impl;
import com.bookeat.menu.dto.*;
import com.bookeat.menu.entity.*;
import com.bookeat.menu.enums.*;
import com.bookeat.menu.mapper.MenuMapper;
import com.bookeat.menu.repository.*;
import com.bookeat.menu.service.*;
import com.bookeat.restaurant.entity.Restaurant;
import com.bookeat.restaurant.service.*;
import com.bookeat.restaurant.mapper.RestaurantMapper;
import com.bookeat.common.response.*;
import com.bookeat.exception.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;
@Service @RequiredArgsConstructor @Transactional
@PreAuthorize("hasRole('OWNER') and hasAuthority('MENU_MANAGE')")
public class MenuServiceImpl implements MenuService {
    private final RestaurantAccessService access;
    private final RestaurantInventoryAccess inventory;
    private final RestaurantChangeService changes;
    private final RestaurantMapper restaurants;
    private final MenuCategoryRepository categories;
    private final MenuItemRepository items;
    private final MenuVariantRepository variants;
    private final MenuAddonRepository addons;
    private final MenuMapper mapper;
    private final Clock clock;
    @Override public RestaurantResource<MenuResponses.Category> createCategory(UUID actor,UUID restaurantId,MenuRequests.Category q) {
        var r=inventory.lockEditable(actor,restaurantId,q.restaurantVersion());
        if(categories.countByRestaurantIdAndDeletedAtIsNull(restaurantId)>=100)throw new ApiException(ErrorCode.INVENTORY_LIMIT_REACHED);
        var c=new MenuCategory();c.setRestaurant(r);assign(c,q);categories.save(c);changes.finish(r,actor,"MENU_CATEGORY_CREATED");return new RestaurantResource<>(r.getVersion(),mapper.category(c));
    }
    @Override @Transactional(readOnly=true) public PageResponse<MenuResponses.Category> categories(UUID actor,UUID restaurantId,int page,int size) {
        access.getOwnedRestaurant(actor,restaurantId);return PageResponse.from(categories.findByRestaurantIdAndDeletedAtIsNull(restaurantId,PageRequest.of(page,size,Sort.by("displayOrder").and(Sort.by("id")))).map(mapper::category));
    }
    @Override @Transactional(readOnly=true) public RestaurantResource<MenuResponses.Category> getCategory(UUID actor,UUID restaurantId,UUID categoryId) {
        var r=access.getOwnedRestaurant(actor,restaurantId);return new RestaurantResource<>(r.getVersion(),mapper.category(category(restaurantId,categoryId)));
    }
    @Override public RestaurantResource<MenuResponses.Category> updateCategory(UUID actor,UUID restaurantId,UUID categoryId,MenuRequests.Category q) {
        var r=inventory.lockEditable(actor,restaurantId,q.restaurantVersion());var c=category(restaurantId,categoryId);assign(c,q);changes.finish(r,actor,"MENU_CATEGORY_UPDATED");return new RestaurantResource<>(r.getVersion(),mapper.category(c));
    }
    @Override public RestaurantResource<UUID> deleteCategory(UUID actor,UUID restaurantId,UUID categoryId,long restaurantVersion) {
        var r=inventory.lockEditable(actor,restaurantId,restaurantVersion);var c=category(restaurantId,categoryId);
        if(items.existsByRestaurantIdAndCategoryIdAndDeletedAtIsNull(restaurantId,categoryId))throw new ApiException(ErrorCode.MENU_CATEGORY_NOT_EMPTY);
        c.setActive(false);c.setDeletedAt(clock.instant());changes.finish(r,actor,"MENU_CATEGORY_ARCHIVED");return new RestaurantResource<>(r.getVersion(),c.getId());
    }
    @Override public RestaurantResource<MenuResponses.Item> createItem(UUID actor,UUID restaurantId,MenuRequests.Item q) {
        var r=inventory.lockEditable(actor,restaurantId,q.restaurantVersion());
        if(items.countByRestaurantIdAndDeletedAtIsNull(restaurantId)>=5000)throw new ApiException(ErrorCode.INVENTORY_LIMIT_REACHED);
        var e=new MenuItem();e.setRestaurant(r);assign(e,q);items.save(e);replaceOptions(e,q);
        return finish(r,actor,e,"MENU_ITEM_CREATED");
    }
    @Override @Transactional(readOnly=true) public PageResponse<MenuResponses.Item> items(UUID actor,UUID restaurantId,UUID categoryId,FoodType foodType,Boolean available,String search,int page,int size) {
        var r=access.getOwnedRestaurant(actor,restaurantId);if(categoryId!=null)category(restaurantId,categoryId);
        Specification<MenuItem> spec=(root,query,cb)->{
            var f=new ArrayList<Predicate>();f.add(cb.equal(root.get("restaurant").get("id"),restaurantId));f.add(cb.isNull(root.get("deletedAt")));
            if(categoryId!=null)f.add(cb.equal(root.get("category").get("id"),categoryId));if(foodType!=null)f.add(cb.equal(root.get("foodType"),foodType));if(available!=null)f.add(cb.equal(root.get("available"),available));
            if(search!=null && !search.isBlank()){var text=search.trim().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_");f.add(cb.like(cb.lower(root.get("name")),"%"+text+"%",'!'));}
            return cb.and(f.toArray(Predicate[]::new));
        };
        var pageData=items.findAll(spec,PageRequest.of(page,size,Sort.by("displayOrder").and(Sort.by("id"))));
        var ids=pageData.stream().map(MenuItem::getId).toList();var enabled=restaurants.enabledServices(r);
        var vs=ids.isEmpty()?Map.<UUID,List<MenuVariant>>of():variants.findByMenuItemIdInAndDeletedAtIsNullOrderByNameAscIdAsc(ids).stream().collect(Collectors.groupingBy(v->v.getMenuItem().getId()));
        var as=ids.isEmpty()?Map.<UUID,List<MenuAddon>>of():addons.findByMenuItemIdInAndDeletedAtIsNullOrderByNameAscIdAsc(ids).stream().collect(Collectors.groupingBy(v->v.getMenuItem().getId()));
        return PageResponse.from(pageData.map(e->mapper.item(e,vs.getOrDefault(e.getId(),List.of()),as.getOrDefault(e.getId(),List.of()),enabled)));
    }
    @Override @Transactional(readOnly=true) public RestaurantResource<MenuResponses.Item> getItem(UUID actor,UUID restaurantId,UUID itemId) {
        var r=access.getOwnedRestaurant(actor,restaurantId);return response(r,item(restaurantId,itemId));
    }
    @Override public RestaurantResource<MenuResponses.Item> updateItem(UUID actor,UUID restaurantId,UUID itemId,MenuRequests.Item q) {
        var r=inventory.lockEditable(actor,restaurantId,q.restaurantVersion());var e=item(restaurantId,itemId);assign(e,q);replaceOptions(e,q);return finish(r,actor,e,"MENU_ITEM_UPDATED");
    }
    @Override public RestaurantResource<UUID> deleteItem(UUID actor,UUID restaurantId,UUID itemId,long restaurantVersion) {
        var r=inventory.lockEditable(actor,restaurantId,restaurantVersion);var e=item(restaurantId,itemId);e.setAvailable(false);e.setDeletedAt(clock.instant());
        variants.findByMenuItemIdAndDeletedAtIsNullOrderByNameAscIdAsc(itemId).forEach(v->{v.setActive(false);v.setDeletedAt(clock.instant());});
        addons.findByMenuItemIdAndDeletedAtIsNullOrderByNameAscIdAsc(itemId).forEach(v->{v.setActive(false);v.setDeletedAt(clock.instant());});
        changes.finish(r,actor,"MENU_ITEM_ARCHIVED");return new RestaurantResource<>(r.getVersion(),e.getId());
    }
    @Override public RestaurantResource<MenuResponses.Item> availability(UUID actor,UUID restaurantId,UUID itemId,MenuRequests.Availability q) {
        var r=inventory.lockEditable(actor,restaurantId,q.restaurantVersion());var e=item(restaurantId,itemId);e.setAvailable(q.available());return finish(r,actor,e,"MENU_AVAILABILITY_UPDATED");
    }
    private MenuCategory category(UUID r,UUID id){return categories.findByIdAndRestaurantIdAndDeletedAtIsNull(id,r).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));}
    private MenuItem item(UUID r,UUID id){return items.findByIdAndRestaurantIdAndDeletedAtIsNull(id,r).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));}
    private void assign(MenuCategory c,MenuRequests.Category q) {
        String name=q.name().trim();if(categories.existsByRestaurantIdAndNameIgnoreCaseAndDeletedAtIsNullAndIdNot(c.getRestaurant().getId(),name,c.getId()==null?new UUID(0,0):c.getId()))throw new ApiException(ErrorCode.MENU_NAME_EXISTS);
        c.setName(name);c.setDescription(q.description());c.setImageUrl(q.imageUrl());c.setDisplayOrder(q.displayOrder());c.setActive(q.active());
    }
    private void assign(MenuItem e,MenuRequests.Item q) {
        if(!MenuPricing.validDiscount(q.basePrice(),q.discountPrice()))throw new ApiException(ErrorCode.VALIDATION_FAILED);
        var c=category(e.getRestaurant().getId(),q.categoryId());String name=q.name().trim();
        if(items.existsByRestaurantIdAndCategoryIdAndNameIgnoreCaseAndDeletedAtIsNullAndIdNot(e.getRestaurant().getId(),c.getId(),name,e.getId()==null?new UUID(0,0):e.getId()))throw new ApiException(ErrorCode.MENU_NAME_EXISTS);
        e.setCategory(c);e.setName(name);e.setDescription(q.description());e.setBasePrice(q.basePrice());e.setDiscountPrice(q.discountPrice());e.setFoodType(q.foodType());e.setSpiceLevel(q.spiceLevel());e.setImageUrl(q.imageUrl());e.setAvailable(q.available());e.setPreOrderAvailable(q.preOrderAvailable());e.setTakeawayAvailable(q.takeawayAvailable());e.setDiniTicketPreOrderAvailable(q.diniTicketPreOrderAvailable());e.setPreparationTimeMinutes(q.preparationTimeMinutes());e.setFeatured(q.featured());e.setDisplayOrder(q.displayOrder());
    }
    private void validateOptions(List<MenuRequests.Option> q,Set<UUID> allowed) {
        var ids=new HashSet<UUID>();var names=new HashSet<String>();
        for(var o:q){if(!names.add(o.name().trim().toLowerCase(Locale.ROOT)) || (o.id()!=null && !ids.add(o.id())))throw new ApiException(ErrorCode.VALIDATION_FAILED);if(o.id()!=null && !allowed.contains(o.id()))throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);}
    }
    private void replaceOptions(MenuItem e,MenuRequests.Item q) {
        var oldV=variants.findByMenuItemIdAndDeletedAtIsNullOrderByNameAscIdAsc(e.getId()).stream().collect(Collectors.toMap(MenuVariant::getId,v->v));
        var oldA=addons.findByMenuItemIdAndDeletedAtIsNullOrderByNameAscIdAsc(e.getId()).stream().collect(Collectors.toMap(MenuAddon::getId,v->v));
        validateOptions(q.variants(),oldV.keySet());validateOptions(q.addons(),oldA.keySet());
        // Clear partial unique-index membership first, allowing safe option-name swaps without changing IDs.
        oldV.values().forEach(v->v.setDeletedAt(clock.instant()));oldA.values().forEach(a->a.setDeletedAt(clock.instant()));variants.flush();addons.flush();
        for(var o:q.variants()){var v=o.id()==null?new MenuVariant():oldV.get(o.id());v.setMenuItem(e);v.setName(o.name().trim());v.setPrice(o.price());v.setActive(o.active());v.setDeletedAt(null);variants.save(v);}
        for(var o:q.addons()){var a=o.id()==null?new MenuAddon():oldA.get(o.id());a.setMenuItem(e);a.setName(o.name().trim());a.setPrice(o.price());a.setActive(o.active());a.setDeletedAt(null);addons.save(a);}
    }
    private RestaurantResource<MenuResponses.Item> response(Restaurant r,MenuItem e){return new RestaurantResource<>(r.getVersion(),mapper.item(e,variants.findByMenuItemIdAndDeletedAtIsNullOrderByNameAscIdAsc(e.getId()),addons.findByMenuItemIdAndDeletedAtIsNullOrderByNameAscIdAsc(e.getId()),restaurants.enabledServices(r)));}
    private RestaurantResource<MenuResponses.Item> finish(Restaurant r,UUID actor,MenuItem e,String action){changes.finish(r,actor,action);return response(r,e);}
    @Override public String uploadItemImage(UUID actor, UUID restaurantId, org.springframework.web.multipart.MultipartFile file, String baseUri) {
        if (file == null || file.isEmpty()) throw new ApiException(ErrorCode.VALIDATION_FAILED);
        access.getOwnedRestaurant(actor, restaurantId);
        String original = file.getOriginalFilename();
        String ext = ".jpg";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        }
        if (!List.of(".jpg", ".jpeg", ".png", ".webp").contains(ext)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED);
        }
        String filename = "menu-" + UUID.randomUUID() + ext;
        try {
            java.nio.file.Path dir = java.nio.file.Path.of(System.getProperty("dinibee.menu.media.root", System.getProperty("user.home") + "/.dinibee/menu-images")).toAbsolutePath().normalize();
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Path target = dir.resolve(filename).normalize();
            java.nio.file.Files.write(target, file.getBytes(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
            return baseUri + "/api/v1/public/media/" + filename;
        } catch (java.io.IOException ex) {
            throw new ApiException(ErrorCode.STORAGE_UNAVAILABLE);
        }
    }
}
