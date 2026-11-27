package com.bookeat.report.service;
import com.bookeat.report.dto.ReportQuery;import com.bookeat.report.enums.ReportType;import com.bookeat.common.response.PageResponse;import com.bookeat.restaurant.service.RestaurantAccessService;import com.bookeat.exception.*;import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.jdbc.core.namedparam.*;import java.util.*;import java.time.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class ReportServiceImpl implements ReportService {
 private final NamedParameterJdbcTemplate jdbc;private final RestaurantAccessService owners;private final Clock clock;private final ReportAuthorization authorization;
 private static final Set<ReportType> OWNER=Set.of(ReportType.RESERVATIONS,ReportType.ORDERS,ReportType.TAKEAWAY,ReportType.DINITICKET,ReportType.DAILY_SALES,ReportType.MONTHLY_SALES,ReportType.TABLE_OCCUPANCY,ReportType.MENU_PERFORMANCE,ReportType.CUSTOMER_COUNT,ReportType.RATINGS,ReportType.REVIEWS,ReportType.REVENUE);
 private record Spec(String table,String columns,String date,String restaurant,String status,String group){}
 private Spec spec(ReportType type){return switch(type){
 case RESERVATIONS->new Spec("reservations x","x.id,x.booking_reference,x.starts_at,x.guest_count,x.booking_type,x.status","x.starts_at","x.restaurant_id","x.status","");
 case ORDERS->new Spec("food_orders x","x.id,x.order_number,x.order_type,x.status,x.subtotal,x.discount,x.tax,x.total","x.created_at","x.restaurant_id","x.status","");
 case TAKEAWAY->new Spec("takeaway_orders x","x.id,x.order_number,x.pickup_time,x.status,x.total,x.payment_status","x.created_at","x.restaurant_id","x.status","");
 case DINITICKET->new Spec("dini_tickets x","x.id,x.ticket_number,x.guest_count,x.status,x.issued_at,x.seated_at,x.completed_at","x.issued_at","x.restaurant_id","x.status","");
 case PAYMENTS->new Spec("payments x","x.id,x.reference_type,x.reference_id,x.amount,x.currency,x.gateway,x.status,x.verified_at,x.refunded_amount","x.created_at","x.restaurant_id","x.status","");
 case SETTLEMENTS->new Spec("settlements x","x.id,x.period_start,x.period_end,x.gross_amount,x.commission_amount,x.refund_amount,x.net_amount,x.status","x.created_at","x.restaurant_id","x.status","");
 case REFUNDS->new Spec("refunds x join payments p on p.id=x.payment_id","x.id,x.payment_id,x.amount,x.status,x.requested_at,x.processed_at","x.requested_at","p.restaurant_id","x.status","");
 case REVIEWS,RATINGS->new Spec("reviews x","x.id,x.rating,x.food_rating,x.service_rating,x.ambience_rating,x.status,x.created_at","x.created_at","x.restaurant_id","x.status","");
 case RESTAURANTS->new Spec("restaurants x","x.id,x.restaurant_name,x.city,x.approval_status,x.status,x.created_at","x.created_at","x.id","x.status","");
 case CUSTOMERS->new Spec("customers x join users u on u.id=x.user_id","x.id,u.first_name,u.last_name,u.status,x.created_at","x.created_at",null,"u.status","");
 case WALLET->new Spec("wallets x","x.id,x.customer_id,x.balance,x.reward_points,x.updated_at","x.updated_at",null,null,"");
 case REWARDS->new Spec("wallet_transactions x","x.id,x.wallet_id,x.transaction_type,x.reference_type,x.reference_id,x.amount,x.points,x.created_at","x.created_at",null,"x.transaction_type","");
 case SUPPORT->new Spec("support_tickets x","x.id,x.ticket_number,x.creator_type,x.category,x.priority,x.status,x.created_at,x.resolved_at","x.created_at",null,"x.status","");
 case REVENUE,DAILY_SALES,MONTHLY_SALES->{String bucket=type==ReportType.MONTHLY_SALES?"month":"day";String date="date_trunc('"+bucket+"',x.verified_at AT TIME ZONE :zone)";yield new Spec("payments x",date+" as period, count(*) as payment_count, sum(x.amount) as captured_amount, sum(x.refunded_amount) as refund_amount,sum(x.amount-x.refunded_amount) as net_sales","x.verified_at","x.restaurant_id","x.status","1");}
 case TABLE_OCCUPANCY->new Spec("dining_sessions x","x.table_id,count(*) as sessions,sum(extract(epoch from (coalesce(x.checked_out_at,:now)-x.checked_in_at))/60) as occupied_minutes","x.checked_in_at","x.restaurant_id","x.status","x.table_id");
 case MENU_PERFORMANCE->new Spec("food_order_items x join food_orders o on o.id=x.order_id left join menu_items mi on mi.id=x.menu_item_id","x.menu_item_id,x.item_name,coalesce(max(x.image_url),max(mi.image_url)) as image_url,sum(x.quantity) as quantity,sum(x.quantity) as order_count,sum(x.subtotal) as net_item_sales","o.created_at","o.restaurant_id","o.status","x.menu_item_id,x.item_name");
 case CUSTOMER_COUNT->new Spec("food_orders x","count(distinct x.customer_id) as customer_count","x.created_at","x.restaurant_id","x.status","()");
 };}
 private MapSqlParameterSource params(UUID actor,UUID owned,ReportQuery q){if(q.fromDate()==null||q.toDate()==null||q.toDate().isBefore(q.fromDate())||java.time.temporal.ChronoUnit.DAYS.between(q.fromDate(),q.toDate())>366)throw new ApiException(ErrorCode.VALIDATION_FAILED);String zone="UTC";if(owned!=null){var r=owners.getOwnedRestaurant(actor,owned);zone=r.getTimezone();if(q.restaurantId()!=null&&!q.restaurantId().equals(owned))throw new ApiException(ErrorCode.ACCESS_DENIED);}return new MapSqlParameterSource().addValue("from",java.sql.Timestamp.from(q.fromDate().atStartOfDay(ZoneId.of(zone)).toInstant())).addValue("until",java.sql.Timestamp.from(q.toDate().plusDays(1).atStartOfDay(ZoneId.of(zone)).toInstant())).addValue("restaurant",owned!=null?owned:q.restaurantId()).addValue("status",q.status()).addValue("service",q.serviceType()==null?null:q.serviceType().name()).addValue("city",q.city()).addValue("zone",zone).addValue("now",java.sql.Timestamp.from(clock.instant()));}
 private String query(ReportType type,ReportQuery q,MapSqlParameterSource p){var s=spec(type);var where=new ArrayList<String>();where.add(s.date()+">=:from and "+s.date()+"<:until");if(p.getValue("restaurant")!=null){if(s.restaurant()==null)throw new ApiException(ErrorCode.VALIDATION_FAILED);where.add(s.restaurant()+"=:restaurant");}if(q.status()!=null){if(s.status()==null)throw new ApiException(ErrorCode.VALIDATION_FAILED);where.add(s.status()+"=:status");}if(q.city()!=null||q.serviceType()!=null){if(s.restaurant()==null)throw new ApiException(ErrorCode.VALIDATION_FAILED);if(q.city()!=null)where.add("exists(select 1 from restaurants venue where venue.id="+s.restaurant()+" and lower(venue.city)=lower(:city))");if(q.serviceType()!=null)where.add("exists(select 1 from restaurant_service_activations activation where activation.restaurant_id="+s.restaurant()+" and activation.service_type=:service and activation.requested and activation.authorized and activation.enabled)");}if(Set.of(ReportType.REVENUE,ReportType.DAILY_SALES,ReportType.MONTHLY_SALES).contains(type))where.add("x.status in ('SUCCESS','PARTIALLY_REFUNDED','REFUNDED')");if(type==ReportType.MENU_PERFORMANCE)where.add("o.status='COMPLETED'");if(type==ReportType.CUSTOMER_COUNT)where.add("x.status='COMPLETED'");if(type==ReportType.TABLE_OCCUPANCY)where.add("x.table_id is not null and x.status<>'CANCELLED'");return "select "+s.columns()+" from "+s.table()+" where "+String.join(" and ",where)+(s.group().isEmpty()?"":" group by "+s.group());}
 public PageResponse<Map<String,Object>> report(UUID actor,UUID owned,ReportType type,ReportQuery q){
authorization.check(type,owned!=null);
 if(owned!=null&&!OWNER.contains(type))throw new ApiException(ErrorCode.ACCESS_DENIED);
 var p=params(actor,owned,q);
 String sql=query(type,q,p);
 int page=q.page()==null?0:q.page(),size=q.size()==null?20:q.size();
 long total=Objects.requireNonNull(jdbc.queryForObject("select count(*) from ("+sql+") result",p,Long.class));p.addValue("limit",size).addValue("offset",(long)page*size);
 var rows=jdbc.queryForList(sql+" order by 1 limit :limit offset :offset",p);
 return new PageResponse<>(rows,page,size,total,(int)((total+size-1)/size),page==0,(long)(page+1)*size>=total);
 }
 public byte[] export(UUID actor,UUID owned,ReportType type,ReportQuery q,String format)
 {
    authorization.check(type,owned!=null);
 if(owned!=null&&!OWNER.contains(type))throw new ApiException(ErrorCode.ACCESS_DENIED);
 var p=params(actor,owned,q);
 var rows=jdbc.queryForList(query(type,q,p)+" order by 1 limit 10001",p);
 if(rows.size()>10000)throw new ApiException(ErrorCode.VALIDATION_FAILED);
 return "csv".equals(format)?ReportExporter.csv(rows):"xlsx".equals(format)?ReportExporter.xlsx(rows):"pdf".equals(format)?ReportExporter.pdf(rows,type.name()):invalid();
 }
 private byte[] invalid(){
    throw new ApiException(ErrorCode.VALIDATION_FAILED);
    }
 public Map<String,Object> dashboard(){
    var p=new MapSqlParameterSource("today",java.sql.Timestamp.from(clock.instant().atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant())).addValue("now",java.sql.Timestamp.from(clock.instant()));
    var r=new LinkedHashMap<String,Object>();
    Map<String,String> counts=Map.ofEntries(
    Map.entry("totalCustomers","select count(*) from customers"),
    Map.entry("totalOwners","select count(*) from restaurant_owners"),
    Map.entry("totalRestaurants","select count(*) from restaurants"),
    Map.entry("pendingRestaurants","select count(*) from restaurants where approval_status in ('SUBMITTED','UNDER_REVIEW')"),
    Map.entry("approvedRestaurants","select count(*) from restaurants where approval_status='APPROVED'"),
    Map.entry("activeRestaurants","select count(*) from restaurants where status='ACTIVE' and approval_status='APPROVED'"),
    Map.entry("totalReservations","select count(*) from reservations"),
    Map.entry("todayReservations","select count(*) from reservations where starts_at>=:today and starts_at<cast(:today as timestamptz)+interval '1 day'"),
    Map.entry("totalOrders","select count(*) from food_orders"),
    Map.entry("todayOrders","select count(*) from food_orders where created_at>=:today and created_at<cast(:today as timestamptz)+interval '1 day'"),
    Map.entry("pendingSettlements","select count(*) from settlements where status in ('PENDING','PROCESSING','ON_HOLD')"),
    Map.entry("refundRequests","select count(*) from refunds where status='REQUESTED'"),
    Map.entry("activeDiniTickets","select count(*) from dini_tickets where status in ('WAITING','CALLED','ARRIVED','SEATED')"),
    Map.entry("openSupportTickets","select count(*) from support_tickets where status not in ('RESOLVED','CLOSED')"),
    Map.entry("todayRegularReservations","select count(*) from reservations where booking_type='REGULAR' and starts_at>=:today and starts_at<cast(:today as timestamptz)+interval '1 day'"),
    Map.entry("todayGroupReservations","select count(*) from reservations where booking_type='GROUP' and starts_at>=:today and starts_at<cast(:today as timestamptz)+interval '1 day'"),
    Map.entry("todayCorporateReservations","select count(*) from reservations where booking_type='CORPORATE' and starts_at>=:today and starts_at<cast(:today as timestamptz)+interval '1 day'"),
    Map.entry("activeDining","select count(*) from reservations where status in ('ARRIVED','CHECKED_IN','DINING')"),
    Map.entry("checkedInReservations","select count(*) from reservations where status='CHECKED_IN'"),
    Map.entry("diningReservations","select count(*) from reservations where status='DINING'"),
    Map.entry("occupiedTables","select count(*) from restaurant_tables where status='OCCUPIED'"),
    Map.entry("activeTakeaways","select count(*) from takeaway_orders where status not in ('COLLECTED','CANCELLED','REJECTED')"),
    Map.entry("onTheWayTakeaways","select count(*) from food_orders where order_type='ON_THE_WAY' and status not in ('COMPLETED','CANCELLED','REJECTED')"),
    Map.entry("waitingDiniTickets","select count(*) from dini_tickets where status in ('WAITING','CALLED','ARRIVED')"),
    Map.entry("openCustomerSupportTickets","select count(*) from support_tickets where creator_type='CUSTOMER' and status not in ('RESOLVED','CLOSED')"),
    Map.entry("openOwnerSupportTickets","select count(*) from support_tickets where creator_type='OWNER' and status not in ('RESOLVED','CLOSED')"),
    Map.entry("pointsIssued","select coalesce(sum(points),0) from wallet_transactions where points>0"),
    Map.entry("pointsRedeemed","select coalesce(sum(abs(points)),0) from wallet_transactions where points<0"));
    counts.forEach((key,sql)->r.put(key,jdbc.queryForObject(sql,p,Long.class)));
    r.put("totalRevenue",jdbc.queryForObject("select coalesce(sum(amount-refunded_amount),0) from payments where status in ('SUCCESS','PARTIALLY_REFUNDED','REFUNDED')",
    p,java.math.BigDecimal.class));
    r.put("platformCommission",jdbc.queryForObject("select coalesce(sum(round(commission_amount*(amount-refunded_amount)/amount,2)),0) from payments where status in ('SUCCESS','PARTIALLY_REFUNDED','REFUNDED')",
    p,java.math.BigDecimal.class));
    for(var pair:Map.of("restaurantGrowth","restaurants","customerGrowth","customers","reservationTrend","reservations").entrySet())
    r.put(pair.getKey(),jdbc.queryForList("select date_trunc('day',created_at AT TIME ZONE 'UTC') as day,count(*) as count from "+pair.getValue()+" where created_at>=cast(:today as timestamptz)-interval '29 days' group by 1 order by 1",p));
    r.put("revenueTrend",jdbc.queryForList("select date_trunc('day',verified_at AT TIME ZONE 'UTC') as day,sum(amount-refunded_amount) as net_sales from payments where verified_at>=cast(:today as timestamptz)-interval '29 days' and status in ('SUCCESS','PARTIALLY_REFUNDED','REFUNDED') group by 1 order by 1",p));
    r.put("asOf",clock.instant());
    r.put("timezone","UTC");
    return r;
    }
}
