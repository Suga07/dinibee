package com.bookeat.paybill;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

/** Restaurant-wise Pay Bill reporting. Amounts count captured/verified payments only. */
@RestController
@RequestMapping("/api/v1/admin/pay-bills")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
@RequiredArgsConstructor
public class PayBillAdminDashboardController {
 private final JdbcTemplate db;
 // Full bill payments and individual split shares are mutually exclusive by payment flow.
 // Aggregating these sources separately avoids treating unpaid bills as revenue.
 private static final String COLLECTED = """
   WITH collected AS (
     SELECT b.restaurant_id, b.id AS bill_id, p.amount_paise, p.verified_at AS paid_at
     FROM pay_bill_payments p JOIN pay_bills b ON b.id=p.pay_bill_id
     WHERE p.status='PAID' AND p.verified_at IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM pay_bill_splits s WHERE s.pay_bill_id=b.id)
     UNION ALL
     SELECT b.restaurant_id, b.id AS bill_id, s.share_paise AS amount_paise, s.paid_at
     FROM pay_bill_splits s JOIN pay_bills b ON b.id=s.pay_bill_id
     WHERE s.status='PAID' AND s.paid_at IS NOT NULL
   )
   """;
 private static final String SUMMARY = """
    SELECT COALESCE((SELECT SUM(amount_paise) FROM collected),0) AS total_amount_paise,
      COALESCE((SELECT SUM(amount_paise) FROM collected WHERE (paid_at AT TIME ZONE 'Asia/Kolkata')::date=(now() AT TIME ZONE 'Asia/Kolkata')::date),0) AS today_amount_paise,
      COALESCE((SELECT SUM(amount_paise) FROM collected WHERE date_trunc('week',paid_at AT TIME ZONE 'Asia/Kolkata')=date_trunc('week',now() AT TIME ZONE 'Asia/Kolkata')),0) AS week_amount_paise,
      (SELECT count(*) FROM pay_bills) AS total_bills,
      (SELECT count(*) FROM pay_bills WHERE status='PAID') AS paid,
      (SELECT count(*) FROM pay_bills WHERE status NOT IN ('PAID','REJECTED')) AS pending,
      (SELECT count(DISTINCT restaurant_id) FROM pay_bills) AS restaurant_count
   """;
 private static final String RSUMMARY = """
    SELECT COALESCE((SELECT SUM(amount_paise) FROM collected WHERE restaurant_id=?),0) AS total_amount_paise,
      COALESCE((SELECT SUM(amount_paise) FROM collected WHERE restaurant_id=? AND (paid_at AT TIME ZONE 'Asia/Kolkata')::date=(now() AT TIME ZONE 'Asia/Kolkata')::date),0) AS today_amount_paise,
      COALESCE((SELECT SUM(amount_paise) FROM collected WHERE restaurant_id=? AND date_trunc('week',paid_at AT TIME ZONE 'Asia/Kolkata')=date_trunc('week',now() AT TIME ZONE 'Asia/Kolkata')),0) AS week_amount_paise,
      (SELECT count(*) FROM pay_bills WHERE restaurant_id=?) AS total_bills,
      (SELECT count(*) FROM pay_bills WHERE restaurant_id=? AND status='PAID') AS paid,
      (SELECT count(*) FROM pay_bills WHERE restaurant_id=? AND status NOT IN ('PAID','REJECTED')) AS pending
   """;
 private void ensureRestaurant(UUID id){
  Integer count=db.queryForObject("SELECT count(*) FROM restaurants WHERE id=?",Integer.class,id);
  if(count==null||count==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Restaurant not found");
 }
 @GetMapping("/summary")
 public Map<String,Object> summary(){
  return db.queryForMap(COLLECTED+SUMMARY);
 }
 @GetMapping("/restaurants")
 public List<Map<String,Object>> restaurants(@RequestParam(defaultValue="") String search,
          @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size){
  int safePage=Math.max(0,page),safeSize=Math.max(1,Math.min(size,100));
  String sql=COLLECTED+"""
    SELECT r.id AS restaurant_id,r.restaurant_name, r.city,
     count(DISTINCT b.id) AS total_bills,
     count(DISTINCT b.id) FILTER(WHERE b.status='PAID') AS paid,
     count(DISTINCT b.id) FILTER(WHERE b.status NOT IN ('PAID','REJECTED')) AS pending,
     COALESCE((SELECT SUM(c.amount_paise) FROM collected c WHERE c.restaurant_id=r.id),0) AS total_amount_paise,
     COALESCE((SELECT SUM(c.amount_paise) FROM collected c WHERE c.restaurant_id=r.id AND (c.paid_at AT TIME ZONE 'Asia/Kolkata')::date=(now() AT TIME ZONE 'Asia/Kolkata')::date),0) AS today_amount_paise,
     COALESCE((SELECT SUM(c.amount_paise) FROM collected c WHERE c.restaurant_id=r.id AND date_trunc('week',c.paid_at AT TIME ZONE 'Asia/Kolkata')=date_trunc('week',now() AT TIME ZONE 'Asia/Kolkata')),0) AS week_amount_paise
    FROM restaurants r JOIN pay_bills b ON b.restaurant_id=r.id
    WHERE (LOWER(r.restaurant_name) LIKE LOWER(?) OR CAST(r.id AS TEXT) LIKE ?)
    GROUP BY r.id,r.restaurant_name,r.city
    ORDER BY total_amount_paise DESC,r.restaurant_name ASC LIMIT ? OFFSET ?
    """;
  String term="%"+search.trim()+"%";
  return db.queryForList(sql,term,term,safeSize,safePage*safeSize);
 }
 @GetMapping("/restaurants/{restaurantId}/summary")
 public Map<String,Object> restaurantSummary(@PathVariable UUID restaurantId){
  ensureRestaurant(restaurantId);
  Map<String,Object> out=new LinkedHashMap<>(db.queryForMap(COLLECTED+RSUMMARY,restaurantId,restaurantId,restaurantId,restaurantId,restaurantId,restaurantId));
  out.putAll(db.queryForMap("SELECT id AS restaurant_id,restaurant_name,city FROM restaurants WHERE id=?",restaurantId));
  return out;
 }
 @GetMapping("/restaurants/{restaurantId}/transactions")
 public List<Map<String,Object>> transactions(@PathVariable UUID restaurantId,@RequestParam(defaultValue="") String search,
       @RequestParam(defaultValue="ALL") String status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size){
  ensureRestaurant(restaurantId);
  int safePage=Math.max(page,0),safeSize=Math.max(1,Math.min(100,size));
  String term="%"+search.trim()+"%";
  return db.queryForList("""
    SELECT b.id,b.bill_number,b.customer_id,b.source,b.amount_paise,b.status,b.created_at,
           b.razorpay_order_id,b.razorpay_payment_id,
           COALESCE((SELECT SUM(s.share_paise) FROM pay_bill_splits s WHERE s.pay_bill_id=b.id AND s.status='PAID'),0) AS split_paid_paise
    FROM pay_bills b WHERE b.restaurant_id=? AND
      (CAST(b.id AS TEXT) ILIKE ? OR b.bill_number ILIKE ? OR CAST(b.customer_id AS TEXT) ILIKE ?)
      AND (?='ALL' OR b.status=?)
    ORDER BY b.created_at DESC LIMIT ? OFFSET ?
    """,restaurantId,term,term,term,status,status,safeSize,safePage*safeSize);
 }
 @GetMapping("/{billId}")
 public Map<String,Object> billDetails(@PathVariable UUID billId){
  var rows=db.queryForList("""
   SELECT b.id,b.bill_number,b.restaurant_id,r.restaurant_name,b.customer_id,b.source,b.amount_paise,
      b.currency,b.status,b.razorpay_order_id,b.razorpay_payment_id,b.created_at,b.updated_at
   FROM pay_bills b JOIN restaurants r ON r.id=b.restaurant_id WHERE b.id=?
  """,billId);
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Bill not found");
  Map<String,Object> out=new LinkedHashMap<>(rows.get(0));
  out.put("payments",db.queryForList("SELECT id,razorpay_order_id,razorpay_payment_id,amount_paise,status,created_at,verified_at FROM pay_bill_payments WHERE pay_bill_id=? ORDER BY created_at DESC",billId));
  out.put("splits",db.queryForList("SELECT id,customer_id,share_paise,status,created_at,paid_at FROM pay_bill_splits WHERE pay_bill_id=? ORDER BY created_at",billId));
  return out;
 }
}
