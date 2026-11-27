package com.bookeat.discovery.service;
import com.bookeat.discovery.dto.DiscoveryDtos.*;
import com.bookeat.discovery.repository.*;
import com.bookeat.discovery.entity.CustomerFavoriteRestaurant;
import com.bookeat.commerce.service.CommerceAccess;
import com.bookeat.common.response.*;
import com.bookeat.restaurant.repository.*;
import com.bookeat.restaurant.enums.ServiceType;
import com.bookeat.restaurant.service.*;
import com.bookeat.menu.repository.*;
import com.bookeat.menu.mapper.MenuMapper;
import com.bookeat.menu.dto.MenuResponses;
import com.bookeat.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import com.bookeat.menu.entity.MenuItem;
import java.time.Clock;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
public class DiscoveryService {
    private final DiscoveryRepository discovery;private final CommerceAccess access;private final RestaurantRepository restaurants;
    private final RestaurantOperatingHourRepository hours;private final RestaurantSpecialScheduleRepository specials;
    private final MenuItemRepository items;private final MenuVariantRepository variants;private final MenuAddonRepository addons;private final MenuMapper menu;
    private final CustomerFavoriteRestaurantRepository favorites;private final RestaurantInventoryAccess inventory;private final RestaurantChangeService changes;private final JdbcTemplate jdbc;private final Clock clock;
    public PageResponse<Card> search(Search q){validate(q);return discovery.search(q,null,null,clock.instant());}
    public PublicDetail detail(UUID id,ServiceType service){
        var rows=discovery.search(empty(service),id,null,clock.instant());
        if(rows.content().isEmpty())throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
        var r=restaurants.findById(id).orElseThrow();
        var zone=java.time.ZoneId.of(r.getTimezone());
        var today=clock.instant().atZone(zone).toLocalDate();
        boolean closedToday=true; java.time.LocalTime opening=null,closing=null;
        var special=specials.findByRestaurantIdOrderByCreatedAtAscIdAsc(id).stream().filter(x->x.getDate().equals(today)).findFirst();
        if(special.isPresent()){
            var h=special.get(); closedToday=h.isClosed(); opening=h.getOpeningTime(); closing=h.getClosingTime();
        }else{
            var weekly=hours.findByRestaurantIdOrderByCreatedAtAscIdAsc(id).stream().filter(x->x.getDayOfWeek()==today.getDayOfWeek()).findFirst();
            if(weekly.isPresent()){var h=weekly.get(); closedToday=h.isClosed(); opening=h.getOpeningTime(); closing=h.getClosingTime();}
        }
        return new PublicDetail(rows.content().get(0),r.getPhone(),r.getAddressLine2(),r.getState(),r.getPostalCode(),r.getCountry(),r.getRestaurantType()==null?null:r.getRestaurantType().name(),opening==null?null:opening.toString(),closing==null?null:closing.toString(),closedToday);
    }
    public Map<String,Object> hours(UUID id,ServiceType service){detail(id,service);var today=clock.instant().atZone(java.time.ZoneId.of(restaurants.findById(id).orElseThrow().getTimezone())).toLocalDate();return Map.of("weekly",hours.findByRestaurantIdOrderByCreatedAtAscIdAsc(id).stream().map(h->new Hours(h.getDayOfWeek().name(),null,h.isClosed(),h.getOpeningTime(),h.getClosingTime(),h.isClosesNextDay())).toList(),"special",specials.findByRestaurantIdOrderByCreatedAtAscIdAsc(id).stream().filter(s->!s.getDate().isBefore(today)).map(s->new Hours(null,s.getDate(),s.isClosed(),s.getOpeningTime(),s.getClosingTime(),s.isClosesNextDay())).toList());}
    public PageResponse<MenuResponses.Item> menu(UUID id,ServiceType service,int page,int size){detail(id,service);var r=restaurants.findById(id).orElseThrow();var enabled=access.enabled(r);var chosen=service==null?ServiceType.DINE_IN:service;
        Specification<MenuItem> spec=(root,query,cb)->{var p=new ArrayList<jakarta.persistence.criteria.Predicate>();p.add(cb.equal(root.get("restaurant").get("id"),id));p.add(cb.isNull(root.get("deletedAt")));p.add(cb.isTrue(root.get("available")));p.add(cb.isTrue(root.get("category").get("active")));p.add(cb.isNull(root.get("category").get("deletedAt")));if(chosen==ServiceType.TAKEAWAY)p.add(cb.isTrue(root.get("takeawayAvailable")));if(chosen==ServiceType.DINITICKET){p.add(cb.isTrue(root.get("diniTicketPreOrderAvailable")));p.add(cb.equal(root.get("category").get("name"),com.bookeat.diniticket.service.DiniTicketSessionMenuService.SESSION_CATEGORY));}else{p.add(cb.notEqual(root.get("category").get("name"),com.bookeat.diniticket.service.DiniTicketSessionMenuService.SESSION_CATEGORY));}return cb.and(p.toArray(jakarta.persistence.criteria.Predicate[]::new));};
        var result=items.findAll(spec,PageRequest.of(page,size,Sort.by("displayOrder","id")));
        var ids=result.getContent().stream().map(MenuItem::getId).toList();
        var itemVariants=ids.isEmpty()?Map.<UUID,List<com.bookeat.menu.entity.MenuVariant>>of():variants.findByMenuItemIdInAndDeletedAtIsNullOrderByNameAscIdAsc(ids).stream().filter(v->v.isActive()).collect(java.util.stream.Collectors.groupingBy(v->v.getMenuItem().getId()));
        var itemAddons=ids.isEmpty()?Map.<UUID,List<com.bookeat.menu.entity.MenuAddon>>of():addons.findByMenuItemIdInAndDeletedAtIsNullOrderByNameAscIdAsc(ids).stream().filter(a->a.isActive()).collect(java.util.stream.Collectors.groupingBy(a->a.getMenuItem().getId()));
        return PageResponse.from(result.map(e->menu.item(e,itemVariants.getOrDefault(e.getId(),List.of()),itemAddons.getOrDefault(e.getId(),List.of()),enabled)));}

    @Transactional public UUID favorite(UUID actor,UUID id){UUID c=access.customer(actor);access.lock(id);if(access.enabled(restaurants.findById(id).orElseThrow()).isEmpty())throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);var f=favorites.findByCustomerIdAndRestaurantId(c,id).orElseGet(()->{var n=new CustomerFavoriteRestaurant();n.setCustomerId(c);n.setRestaurantId(id);return favorites.save(n);});return f.getRestaurantId();}
    @Transactional public void unfavorite(UUID actor,UUID id){var c=access.customer(actor);access.lock(id);favorites.findByCustomerIdAndRestaurantId(c,id).ifPresent(favorites::delete);}
    public PageResponse<Card> favorites(UUID actor,Search q){validate(q);return discovery.search(q,null,access.customer(actor),clock.instant());}
    @Transactional public RestaurantResource<List<String>> cuisines(UUID actor,UUID id,Cuisines q){var r=inventory.lockEditable(actor,id,q.restaurantVersion());var names=q.cuisines().stream().map(String::trim).toList();if(names.stream().map(s->s.toLowerCase(Locale.ROOT)).distinct().count()!=names.size())throw new ApiException(ErrorCode.VALIDATION_FAILED);jdbc.update("delete from restaurant_cuisines where restaurant_id=?",id);for(String name:names)jdbc.update("insert into restaurant_cuisines(restaurant_id,name) values (?,?)",id,name);changes.finish(r,actor,"RESTAURANT_CUISINES_UPDATED");return new RestaurantResource<>(r.getVersion(),names);}
    private void validate(Search q){if((q.latitude()==null)!=(q.longitude()==null)||(q.radius()!=null&&q.latitude()==null)||("DISTANCE".equals(q.sort())&&q.latitude()==null)||(q.latitude()!=null&&(!Double.isFinite(q.latitude())||!Double.isFinite(q.longitude()))))throw new ApiException(ErrorCode.VALIDATION_FAILED);}
    private Search empty(ServiceType s){return new Search(null,null,null,null,null,null,null,null,s,null,null,null,0,20);}
    public record Hours(String dayOfWeek,java.time.LocalDate date,boolean closed,java.time.LocalTime openingTime,java.time.LocalTime closingTime,boolean closesNextDay) {}
}
