package com.bookeat.discovery.repository;
import com.bookeat.discovery.dto.DiscoveryDtos.*;
import com.bookeat.common.response.PageResponse;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;
import java.sql.*;
import java.time.Instant;
import java.util.*;
@Repository @RequiredArgsConstructor
public class DiscoveryRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public PageResponse<Card> search(Search q,UUID onlyId,UUID favoriteCustomer,Instant now) {
        int page=q.page()==null?0:q.page(),size=q.size()==null?20:q.size();
        var p=new MapSqlParameterSource("moment",Timestamp.from(now)).addValue("limit",size).addValue("offset",(long)page*size);
        String distance="null::double precision";
        if(q.latitude()!=null){p.addValue("lat",q.latitude()).addValue("lon",q.longitude());distance="CASE WHEN r.latitude IS NULL OR r.longitude IS NULL THEN NULL ELSE 6371.0088*2*asin(sqrt(least(1.0,greatest(0.0,power(sin(radians(r.latitude::double precision-:lat)/2),2)+cos(radians(:lat))*cos(radians(r.latitude::double precision))*power(sin(radians(r.longitude::double precision-:lon)/2),2))))) END";}
        String base="SELECT r.*, " +
                "coalesce(r.logo_url,(SELECT '/api/v1/public/restaurants/'||r.id||'/media/'||d.id FROM restaurant_documents d WHERE d.restaurant_id=r.id AND d.document_type='LOGO' AND d.deleted_at IS NULL ORDER BY d.created_at DESC,d.id DESC LIMIT 1)) effective_logo_url, " +
                "coalesce(r.cover_image_url,(SELECT '/api/v1/public/restaurants/'||r.id||'/media/'||d.id FROM restaurant_documents d WHERE d.restaurant_id=r.id AND d.document_type='COVER_IMAGE' AND d.deleted_at IS NULL ORDER BY d.created_at DESC,d.id DESC LIMIT 1)) effective_cover_image_url, " +
                distance+" distance_km, coalesce(nullif(rt.average_rating,0),(SELECT round(avg(rev.rating)::numeric,1) FROM reviews rev WHERE rev.restaurant_id=r.id AND rev.status='VISIBLE'),0) average_rating, coalesce(nullif(rt.review_count,0),(SELECT count(*) FROM reviews rev WHERE rev.restaurant_id=r.id AND rev.status='VISIBLE'),0) review_count, restaurant_open_at(r.id,:moment) open_now, EXISTS(SELECT 1 FROM menu_items m JOIN menu_categories c ON c.id=m.category_id WHERE m.restaurant_id=r.id AND m.deleted_at IS NULL AND m.available AND c.deleted_at IS NULL AND c.active AND (:service='DINE_IN' OR (:service='TAKEAWAY' AND m.takeaway_available) OR (:service='DINITICKET' AND m.dini_ticket_pre_order_available)) AND m.discount_price<m.base_price) offer_available, ARRAY(SELECT c.name FROM restaurant_cuisines c WHERE c.restaurant_id=r.id ORDER BY c.name) cuisines, ARRAY(SELECT a.service_type FROM restaurant_service_activations a WHERE a.restaurant_id=r.id AND a.requested AND a.authorized AND a.enabled ORDER BY a.service_type) enabled_services FROM restaurants r LEFT JOIN restaurant_rating_summaries rt ON rt.restaurant_id=r.id WHERE r.approval_status='APPROVED' AND r.status='ACTIVE'";
        var filters=new ArrayList<String>();filters.add(":service=ANY(enabled_services)");p.addValue("service",q.serviceType()==null?"DINE_IN":q.serviceType().name());
        if(onlyId!=null){filters.add("id=:id");p.addValue("id",onlyId);}
        if(favoriteCustomer!=null){filters.add("EXISTS(SELECT 1 FROM customer_favorite_restaurants f WHERE f.restaurant_id=visible.id AND f.customer_id=:customer)");p.addValue("customer",favoriteCustomer);}
        if(q.restaurantName()!=null&&!q.restaurantName().isBlank()){filters.add("lower(restaurant_name) LIKE :name ESCAPE '!'");p.addValue("name","%"+literal(q.restaurantName())+"%");}
        if(q.city()!=null&&!q.city().isBlank()){filters.add("lower(city)=:city");p.addValue("city",q.city().trim().toLowerCase(Locale.ROOT));}
        if(q.radius()!=null){filters.add("distance_km<=:radius");p.addValue("radius",q.radius());}
        if(q.cuisine()!=null&&!q.cuisine().isBlank()){filters.add("EXISTS(SELECT 1 FROM restaurant_cuisines c WHERE c.restaurant_id=visible.id AND lower(c.name)=:cuisine)");p.addValue("cuisine",q.cuisine().trim().toLowerCase(Locale.ROOT));}
        if(q.rating()!=null){filters.add("average_rating>=:rating");p.addValue("rating",q.rating());}
        if(q.foodType()!=null){filters.add("EXISTS(SELECT 1 FROM menu_items m JOIN menu_categories c ON c.id=m.category_id WHERE m.restaurant_id=visible.id AND m.deleted_at IS NULL AND m.available AND c.deleted_at IS NULL AND c.active AND (:service='DINE_IN' OR (:service='TAKEAWAY' AND m.takeaway_available) OR (:service='DINITICKET' AND m.dini_ticket_pre_order_available)) AND m.food_type=:food)");p.addValue("food",q.foodType().name());}
        if(q.openNow()!=null){filters.add("open_now=:open");p.addValue("open",q.openNow());}
        if(q.offerAvailable()!=null){filters.add("offer_available=:offer");p.addValue("offer",q.offerAvailable());}
        String from=" FROM ("+base+") visible WHERE "+String.join(" AND ",filters);
        long count=Objects.requireNonNull(jdbc.queryForObject("SELECT count(*)"+from,p,Long.class));
        String sort="RATING".equals(q.sort())?"average_rating DESC,review_count DESC,restaurant_name,id":"DISTANCE".equals(q.sort())?"distance_km ASC NULLS LAST,restaurant_name,id":"restaurant_name,id";
        var rows=jdbc.query("SELECT *"+from+" ORDER BY "+sort+" LIMIT :limit OFFSET :offset",p,(rs,n)->card(rs));
        return new PageResponse<>(rows,page,size,count,(int)((count+size-1)/size),page==0,(long)(page+1)*size>=count);
    }
    private static String literal(String s){return s.trim().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_");}
    private static List<String> strings(java.sql.Array a)throws SQLException{try{return List.of((String[])a.getArray());}finally{a.free();}}
    private static Card card(ResultSet rs)throws SQLException{return new Card(rs.getObject("id",UUID.class),rs.getString("restaurant_name"),rs.getString("description"),rs.getString("city"),rs.getString("address_line1"),rs.getString("effective_logo_url"),rs.getString("effective_cover_image_url"),rs.getBigDecimal("latitude"),rs.getBigDecimal("longitude"),rs.getString("timezone"),(Double)rs.getObject("distance_km"),rs.getBigDecimal("average_rating"),rs.getLong("review_count"),rs.getBoolean("open_now"),rs.getBoolean("offer_available"),strings(rs.getArray("cuisines")),strings(rs.getArray("enabled_services")));}
}
