package com.bookeat.admin.controller;

import com.bookeat.common.response.ApiResponse;
import com.bookeat.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** Admin read models backed by the same production tables used by customer and owner apps. */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminLiveReadController {
    private final NamedParameterJdbcTemplate jdbc;

    private ApiResponse<PageResponse<Map<String,Object>>> page(String sql) {
        var rows = jdbc.queryForList(sql, Map.of());
        return ApiResponse.ok("Live data loaded", new PageResponse<>(rows,0,Math.max(rows.size(),1),rows.size(),1,true,true));
    }

    @GetMapping("/reservations") @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ApiResponse<PageResponse<Map<String,Object>>> reservations(@RequestParam(required=false) String bookingType) {
        String where = bookingType == null || bookingType.isBlank() ? "" : " where r.booking_type = :bookingType ";
        var rows = jdbc.queryForList("""
          select r.id, r.booking_reference as "bookingNumber", r.booking_type as "bookingType", r.customer_id as "customerId",
          concat_ws(' ',u.first_name,u.last_name) as "customerName", u.phone_number as "customerPhone", u.email as "customerEmail",
          r.restaurant_id as "restaurantId", v.restaurant_name as "restaurantName",
          coalesce(v.logo_url,(select '/api/v1/public/restaurants/'||v.id||'/media/'||d.id from restaurant_documents d where d.restaurant_id=v.id and d.document_type='LOGO' and d.deleted_at is null order by d.created_at desc,d.id desc limit 1)) as "restaurantLogoUrl",
          coalesce(v.cover_image_url,(select '/api/v1/public/restaurants/'||v.id||'/media/'||d.id from restaurant_documents d where d.restaurant_id=v.id and d.document_type='COVER_IMAGE' and d.deleted_at is null order by d.created_at desc,d.id desc limit 1)) as "restaurantCoverImageUrl",
          concat_ws(', ',nullif(v.address_line1,''),nullif(v.city,''),nullif(v.state,'')) as "restaurantLocation",
          coalesce(rt.table_number, r.table_id::text, '—') as "tableId", r.reservation_date as date, r.start_time as time, r.end_time as "endTime", r.guest_count as "guestCount",
          r.special_request as "specialRequest", r.created_at as "createdAt", r.updated_at as "updatedAt",
          case when r.status in ('ARRIVED','CHECKED_IN','DINING','COMPLETED') then 'ARRIVED' else 'NOT_ARRIVED' end as "arrivalStatus",
          r.status, r.payment_status as "paymentStatus", 0::numeric as amount,
          case when r.status='DINING' then 'OCCUPIED' when r.status in ('CONFIRMED','ARRIVED','CHECKED_IN') then 'RESERVED' else 'AVAILABLE' end as "tableStatus",
          0 as "rewardPoints", 'PENDING' as "reviewStatus", r.checked_in_at as "checkInTime", r.completed_at as "checkOutTime"
          from reservations r join customers c on c.id=r.customer_id join users u on u.id=c.user_id join restaurants v on v.id=r.restaurant_id
          left join restaurant_tables rt on rt.id=r.table_id
          """ + where + " order by r.created_at desc", bookingType == null || bookingType.isBlank() ? Map.of() : Map.of("bookingType", bookingType));
        return ApiResponse.ok("Live reservations loaded", new PageResponse<>(rows,0,Math.max(rows.size(),1),rows.size(),1,true,true));
    }


    @GetMapping("/reservations/{id}/history") @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ApiResponse<List<Map<String,Object>>> reservationHistory(@PathVariable UUID id) {
        var rows = jdbc.queryForList("""
          select h.id, h.old_status as "oldStatus", h.new_status as "newStatus", h.reason, h.changed_at as "changedAt",
          concat_ws(' ',u.first_name,u.last_name) as "changedByName"
          from reservation_status_history h left join users u on u.id=h.changed_by
          where h.reservation_id=:id order by h.changed_at desc
        """, Map.of("id", id));
        return ApiResponse.ok("Reservation history loaded", rows);
    }

    @GetMapping("/food-orders") @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ApiResponse<PageResponse<Map<String,Object>>> foodOrders() {
        return page("""
          select o.id,o.order_number as "orderNumber",coalesce(r.booking_reference,'—') as "bookingNumber",
          concat_ws(' ',u.first_name,u.last_name) as "customerName",v.restaurant_name as "restaurantName",
          coalesce(rt.table_number,r.table_id::text,'—') as "tableId",coalesce(r.booking_type,'REGULAR') as "bookingType",coalesce(o.total,0) as amount,
          o.status as "orderStatus",coalesce((select p.status from payments p where p.reference_id=o.id order by p.created_at desc limit 1),'UNPAID') as "paymentStatus",
          o.created_at as "createdAt"
          from food_orders o join customers c on c.id=o.customer_id join users u on u.id=c.user_id join restaurants v on v.id=o.restaurant_id
          left join reservations r on r.id=o.reservation_id left join restaurant_tables rt on rt.id=r.table_id order by o.created_at desc
        """);
    }

    @GetMapping("/diniticket/orders") @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ApiResponse<PageResponse<Map<String,Object>>> diniTickets() {
        return page("""
          select d.id,d.ticket_number as "tokenNumber",d.customer_id as "customerId",c.user_id as "customerUserId",
          concat_ws(' ',u.first_name,u.last_name) as "customerName",u.phone_number as "customerPhone",u.email as "customerEmail",
          d.restaurant_id as "restaurantId",v.restaurant_name as "restaurantName",
          coalesce(v.logo_url,(select '/api/v1/public/restaurants/'||v.id||'/media/'||rd.id from restaurant_documents rd where rd.restaurant_id=v.id and rd.document_type='LOGO' and rd.deleted_at is null order by rd.created_at desc,rd.id desc limit 1)) as "restaurantLogoUrl",
          coalesce(v.cover_image_url,(select '/api/v1/public/restaurants/'||v.id||'/media/'||rd.id from restaurant_documents rd where rd.restaurant_id=v.id and rd.document_type='COVER_IMAGE' and rd.deleted_at is null order by rd.created_at desc,rd.id desc limit 1)) as "restaurantCoverImageUrl",
          concat_ws(', ',nullif(v.address_line1,''),nullif(v.city,''),nullif(v.state,'')) as "restaurantLocation",
          coalesce(o.order_number,'—') as "orderNumber",o.id as "foodOrderId",coalesce(o.total,0) as amount,
          coalesce((select p.status from payments p where p.reference_id=o.id order by p.created_at desc limit 1),'UNPAID') as "paymentStatus",
          (select p.id from payments p where p.reference_id=o.id order by p.created_at desc limit 1) as "paymentId",
          (select foi.image_url from food_order_items foi where foi.order_id=o.id and nullif(btrim(foi.image_url),'') is not null order by foi.created_at desc,foi.id desc limit 1) as "foodImageUrl",
          coalesce((select string_agg(foi.item_name||' ×'||foi.quantity, ', ' order by foi.created_at,foi.id) from food_order_items foi where foi.order_id=o.id),'—') as "itemsSummary",
          d.status as "queueStatus",case when d.arrived_at is not null then 'ARRIVED' else 'NOT_STARTED' end as "arrivalStatus",
          d.issued_at as "createdAt",d.estimated_wait_minutes as "waitMinutes"
          from dini_tickets d join customers c on c.id=d.customer_id join users u on u.id=c.user_id join restaurants v on v.id=d.restaurant_id
          left join food_orders o on o.dini_ticket_id=d.id order by d.issued_at desc
        """);
    }

    @GetMapping("/reviews") @PreAuthorize("hasAuthority('REVIEW_MODERATE') or hasAuthority('REPORT_VIEW')")
    public ApiResponse<PageResponse<Map<String,Object>>> reviews() {
        return page("""
          select x.id,concat_ws(' ',u.first_name,u.last_name) as \"customerName\",v.restaurant_name as \"restaurantName\",
          coalesce(r.booking_reference,o.order_number,x.id::text) as \"referenceId\",x.rating as \"restaurantRating\",
          coalesce(x.food_rating,x.rating) as \"foodQuality\",coalesce(x.service_rating,x.rating) as service,
          coalesce(x.ambience_rating,x.rating) as ambience,x.rating as \"valueForMoney\",coalesce(x.comment,'') as \"reviewText\",
          0 as \"imageCount\",x.created_at as \"createdAt\",x.status
          from reviews x join customers c on c.id=x.customer_id join users u on u.id=c.user_id join restaurants v on v.id=x.restaurant_id
          left join reservations r on r.id=x.reservation_id left join food_orders o on o.id=x.food_order_id order by x.created_at desc
        """);
    }

    @GetMapping("/takeaway-orders") @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ApiResponse<PageResponse<Map<String,Object>>> takeaway(@RequestParam(required=false) String type){
        String where = type == null || type.isBlank() ? "" : " where o.order_type = :type ";
        var rows=jdbc.queryForList("""
          select t.id,t.order_number as "orderNumber",o.order_type as type,t.customer_id as "customerId",c.user_id as "customerUserId",
          concat_ws(' ',u.first_name,u.last_name) as "customerName",u.phone_number as "customerPhone",u.email as "customerEmail",
          t.restaurant_id as "restaurantId",v.restaurant_name as "restaurantName",
          coalesce(v.logo_url,(select '/api/v1/public/restaurants/'||v.id||'/media/'||rd.id from restaurant_documents rd where rd.restaurant_id=v.id and rd.document_type='LOGO' and rd.deleted_at is null order by rd.created_at desc,rd.id desc limit 1)) as "restaurantLogoUrl",
          coalesce(v.cover_image_url,(select '/api/v1/public/restaurants/'||v.id||'/media/'||rd.id from restaurant_documents rd where rd.restaurant_id=v.id and rd.document_type='COVER_IMAGE' and rd.deleted_at is null order by rd.created_at desc,rd.id desc limit 1)) as "restaurantCoverImageUrl",
          concat_ws(', ',nullif(v.address_line1,''),nullif(v.city,''),nullif(v.state,'')) as "restaurantLocation",
          t.food_order_id as "foodOrderId",
          (select p.id from payments p where p.reference_id in (t.id,o.id) order by p.created_at desc limit 1) as "paymentId",
          (select p.payment_method from payments p where p.reference_id in (t.id,o.id) order by p.created_at desc limit 1) as "paymentMethod",
          (select foi.image_url from food_order_items foi where foi.order_id=o.id and nullif(btrim(foi.image_url),'') is not null order by foi.created_at desc,foi.id desc limit 1) as "foodImageUrl",
          coalesce((select string_agg(foi.item_name||' ×'||foi.quantity, ', ' order by foi.created_at,foi.id) from food_order_items foi where foi.order_id=o.id),'—') as "itemsSummary",
          case when o.order_type='ON_THE_WAY' then case when t.status in ('COLLECTED','COMPLETED') then 'ARRIVED' else 'NOT_STARTED' end else 'NOT_STARTED' end as "journeyStatus",
          null::text as eta,coalesce(t.total,0) as amount,coalesce(t.payment_status::text,'UNPAID') as "paymentStatus",
          coalesce((select case when rf.status='REFUNDED' then 'REFUNDED' else 'REQUESTED' end from refunds rf join payments p on p.id=rf.payment_id where p.reference_id in (t.id,o.id) order by rf.requested_at desc limit 1),'NONE') as "refundStatus",
          t.status as status,t.created_at as "createdAt"
          from takeaway_orders t join food_orders o on o.id=t.food_order_id join customers c on c.id=t.customer_id
          join users u on u.id=c.user_id join restaurants v on v.id=t.restaurant_id
          """+where+" order by t.created_at desc", type == null || type.isBlank() ? Map.of() : Map.of("type", type));
        return ApiResponse.ok("Live takeaway orders loaded",new PageResponse<>(rows,0,Math.max(rows.size(),1),rows.size(),1,true,true));
    }

    @GetMapping("/finance/payments") @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ApiResponse<PageResponse<Map<String,Object>>> payments(){return page("""
      select p.id,coalesce(p.gateway_payment_id,substring(p.id::text,1,8)) as "transactionId",p.reference_id::text as "referenceId",
      concat_ws(' ',u.first_name,u.last_name) as "customerName",v.restaurant_name as "restaurantName",
      case p.reference_type when 'FOOD_ORDER' then 'DINE_IN' else p.reference_type::text end as "paymentType",
      coalesce(p.amount,0)+coalesce(p.discount_amount,0) as "grossAmount",coalesce(p.discount_amount,0) as "rewardDiscount",
      coalesce(p.amount,0) as "gatewayPaidAmount",coalesce(p.payment_method,p.gateway::text,'—') as "paymentMethod",p.status,p.created_at as date
      from payments p join customers c on c.id=p.customer_id join users u on u.id=c.user_id join restaurants v on v.id=p.restaurant_id
      order by p.created_at desc
    """);}
    @GetMapping("/notification-broadcasts") @PreAuthorize("hasAuthority('NOTIFICATION_MANAGE')")
    public ApiResponse<PageResponse<Map<String,Object>>> notificationBroadcasts(){return page("""
      select n.reference_id::text as id,max(n.title) as title,max(n.message) as message,max(n.notification_type) as type,
      'BROADCAST' as audience,count(*) as "recipientCount",'Admin' as "createdBy",min(n.created_at) as "createdAt",'SENT' as status
      from notifications n where n.reference_type='ADMIN_BROADCAST' group by n.reference_id order by min(n.created_at) desc
    """);}

    @GetMapping("/finance/refunds") public ApiResponse<PageResponse<Map<String,Object>>> refunds(){return page("select * from refunds order by requested_at desc");}
    @GetMapping("/finance/settlements") public ApiResponse<PageResponse<Map<String,Object>>> settlements(){return page("select * from settlements order by created_at desc");}
    @GetMapping("/corporate-invoices") public ApiResponse<PageResponse<Map<String,Object>>> invoices(){return page("select * from corporate_invoices order by created_at desc");}
}
