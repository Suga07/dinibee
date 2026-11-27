package com.bookeat.paybill;

import com.bookeat.security.AppPrincipal;
import com.bookeat.payment.gateway.RazorpayPaymentsGateway;
import com.bookeat.payment.gateway.GatewayPort.GatewayOrder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.*;

/** Additional Pay Bill endpoints; existing Takeaway/DiniTicket Razorpay paths are untouched. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PayBillExtrasController {
 private final JdbcTemplate db;
 private final RazorpayPaymentsGateway razorpay;
 public record SplitRequest(@NotEmpty List<@NotNull UUID> customerIds) {}
 public record VerifyRequest(@NotBlank String paymentId,@NotBlank String signature) {}
 public record OfferRequest(@NotBlank String code,@NotBlank String title,@NotBlank String offerType,
  UUID restaurantId, boolean premiumOnly,boolean active,@DecimalMin("0") @DecimalMax("100") BigDecimal cashbackPercent,
  @Min(0) long maxCashbackPaise,@Min(0) long minBillPaise,String eligibleBank,
  @NotNull java.time.OffsetDateTime startsAt,@NotNull java.time.OffsetDateTime endsAt) {}
 private ResponseStatusException conflict(String text){return new ResponseStatusException(HttpStatus.CONFLICT,text);}
 private UUID customer(AppPrincipal p){
  UUID id=db.query("SELECT id FROM customers WHERE user_id=?", rs->rs.next()?rs.getObject(1,UUID.class):null,p.userId());
  if(id==null)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Customer profile not found");return id;
 }
 private Map<String,Object> billLocked(UUID billId){
  var xs=db.queryForList("SELECT id,restaurant_id,customer_id,amount_paise,status,bill_number FROM pay_bills WHERE id=? FOR UPDATE",billId);
  if(xs.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Bill not found");return xs.get(0);
 }
 private boolean participant(UUID billId, UUID customerId){
  Integer n=db.queryForObject("SELECT count(*) FROM pay_bill_splits WHERE pay_bill_id=? AND customer_id=?",Integer.class,billId,customerId);
  return n!=null&&n>0;
 }
 @PostMapping("/customer/pay-bills/{billId}/splits")
 @PreAuthorize("hasRole('CUSTOMER')") @Transactional
 public List<Map<String,Object>> split(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID billId,@Valid @RequestBody SplitRequest req){
  var b=billLocked(billId);UUID caller=customer(p);
  if(!caller.equals(b.get("customer_id")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only bill host can split");
  if(!"READY_TO_PAY".equals(b.get("status")))throw conflict("Bill must be verified and unpaid");
  LinkedHashSet<UUID> ids=new LinkedHashSet<>(req.customerIds()); ids.add(caller);
  if(ids.size()<2||ids.size()>20)throw conflict("Split requires 2-20 unique participants");
  Integer existing=db.queryForObject("SELECT count(*) FROM pay_bill_splits WHERE pay_bill_id=?",Integer.class,billId);
  if(existing!=null&&existing>0)throw conflict("Split already created");
  Integer valid=db.queryForObject("SELECT count(*) FROM customers WHERE id IN ("+String.join(",",Collections.nCopies(ids.size(),"?"))+")",Integer.class,ids.toArray());
  if(valid==null||valid!=ids.size())throw conflict("One or more guests must register before splitting");
  long amount=((Number)b.get("amount_paise")).longValue();if(amount<ids.size())throw conflict("Amount too small to split");
  long base=amount/ids.size(), remainder=amount%ids.size();int i=0;
  for(UUID id:ids){db.update("INSERT INTO pay_bill_splits(id,pay_bill_id,customer_id,share_paise) VALUES(?,?,?,?)",UUID.randomUUID(),billId,id,base+(i++<remainder?1:0));}
  return splitsInternal(billId);
 }
 private List<Map<String,Object>> splitsInternal(UUID id){return db.queryForList("SELECT id,customer_id,share_paise,status,paid_at FROM pay_bill_splits WHERE pay_bill_id=? ORDER BY created_at",id);}
 @GetMapping("/customer/pay-bills/{billId}/splits") @PreAuthorize("hasRole('CUSTOMER')")
 public List<Map<String,Object>> splits(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID billId){
  UUID c=customer(p);Map<String,Object>b=billLocked(billId);
  if(!c.equals(b.get("customer_id"))&&!participant(billId,c))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Not your split");
  return splitsInternal(billId);
 }
 @PostMapping("/customer/pay-bills/{billId}/splits/{splitId}/payment-order")
 @PreAuthorize("hasRole('CUSTOMER')") @Transactional
 public Map<String,Object> splitOrder(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID billId,@PathVariable UUID splitId){
  UUID c=customer(p);var b=billLocked(billId);
  if(!"READY_TO_PAY".equals(b.get("status"))&&!"PAYMENT_PENDING".equals(b.get("status")))throw conflict("Bill is not payable");
  var list=db.queryForList("SELECT id,share_paise,status,razorpay_order_id FROM pay_bill_splits WHERE id=? AND pay_bill_id=? AND customer_id=? FOR UPDATE",splitId,billId,c);
  if(list.isEmpty())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Not your split share");var s=list.get(0);
  if("PAID".equals(s.get("status")))throw conflict("Share already paid");
  long minor=((Number)s.get("share_paise")).longValue();
  if(s.get("razorpay_order_id")!=null)return Map.of("orderId",s.get("razorpay_order_id"),"key",razorpay.publicKey(),"amountPaise",minor,"splitId",splitId);
  GatewayOrder order=razorpay.create(splitId,BigDecimal.valueOf(minor,2));
  db.update("UPDATE pay_bill_splits SET razorpay_order_id=?,status='PAYMENT_PENDING' WHERE id=?",order.id(),splitId);
  db.update("UPDATE pay_bills SET status='PAYMENT_PENDING',updated_at=now() WHERE id=? AND status='READY_TO_PAY'",billId);
  return Map.of("orderId",order.id(),"key",razorpay.publicKey(),"amountPaise",minor,"splitId",splitId);
 }
 @PostMapping("/customer/pay-bills/{billId}/splits/{splitId}/verify-payment")
 @PreAuthorize("hasRole('CUSTOMER')") @Transactional
 public Map<String,Object> verify(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID billId,@PathVariable UUID splitId,@Valid @RequestBody VerifyRequest req){
  UUID c=customer(p);var b=billLocked(billId);
  var list=db.queryForList("SELECT id,share_paise,status,razorpay_order_id FROM pay_bill_splits WHERE id=? AND pay_bill_id=? AND customer_id=? FOR UPDATE",splitId,billId,c);
  if(list.isEmpty())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Not your share");var s=list.get(0);
  if("PAID".equals(s.get("status")))return Map.of("status",b.get("status"),"shareStatus","PAID");
  String orderId=(String)s.get("razorpay_order_id");if(orderId==null)throw conflict("Missing payment order");
  if(!razorpay.verifyCheckout(orderId,req.paymentId(),req.signature()))throw conflict("Payment signature invalid");
  var payment=razorpay.fetchPayment(req.paymentId());
  if(!orderId.equals(payment.orderId())||!"captured".equalsIgnoreCase(payment.status())||!"INR".equals(payment.currency())||payment.amount()!=((Number)s.get("share_paise")).longValue())throw conflict("Payment not captured for the allocated share");
  db.update("UPDATE pay_bill_splits SET status='PAID',razorpay_payment_id=?,paid_at=now() WHERE id=? AND status='PAYMENT_PENDING'",req.paymentId(),splitId);
  Long paid=db.queryForObject("SELECT COALESCE(sum(share_paise),0) FROM pay_bill_splits WHERE pay_bill_id=? AND status='PAID'",Long.class,billId);
  long total=((Number)b.get("amount_paise")).longValue();
  if(paid!=null&&paid==total)db.update("UPDATE pay_bills SET status='PAID',updated_at=now() WHERE id=? AND status='PAYMENT_PENDING'",billId);
  return Map.of("status",paid!=null&&paid==total?"PAID":"PARTIALLY_PAID","paidPaise",paid==null?0:paid,"totalPaise",total);
 }
 @GetMapping("/customer/pay-bills/{billId}/receipt") @PreAuthorize("hasRole('CUSTOMER')")
 public Map<String,Object> receipt(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID billId){
  UUID c=customer(p);
  var xs=db.queryForList("SELECT b.id,b.bill_number,b.restaurant_id,b.amount_paise,b.currency,b.status,b.razorpay_payment_id,b.created_at FROM pay_bills b WHERE b.id=? AND (b.customer_id=? OR EXISTS(SELECT 1 FROM pay_bill_splits s WHERE s.pay_bill_id=b.id AND s.customer_id=?))",billId,c,c);
  if(xs.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Receipt not found");
  var bill=xs.get(0);if(!"PAID".equals(bill.get("status")))throw conflict("Receipt available after full settlement only");
  return Map.of("bill",bill,"splitPayments",db.queryForList("SELECT share_paise,razorpay_payment_id,paid_at FROM pay_bill_splits WHERE pay_bill_id=? AND status='PAID'",billId));
 }
 @GetMapping("/customer/pay-bill-offers") @PreAuthorize("hasRole('CUSTOMER')")
 public List<Map<String,Object>> offers(@AuthenticationPrincipal AppPrincipal p,@RequestParam UUID restaurantId,@RequestParam long amountPaise){
  if(amountPaise<=0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Bill amount must be positive");
  UUID c=customer(p);
  // Premium policy uses current active memberships: extend this clause only after validating membership schema.
  return db.queryForList("SELECT id,code,title,offer_type,cashback_percent,max_cashback_paise,min_bill_paise,eligible_bank,premium_only FROM pay_bill_offers WHERE active=true AND premium_only=false AND (restaurant_id IS NULL OR restaurant_id=?) AND starts_at<=now() AND ends_at>now() AND min_bill_paise<=? ORDER BY created_at DESC",restaurantId,amountPaise);
 }
 public record SelectOfferRequest(@NotNull UUID offerId) {}
 @PutMapping("/customer/pay-bills/{billId}/offer") @PreAuthorize("hasRole('CUSTOMER')") @Transactional
 public Map<String,Object> selectOffer(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID billId,@Valid @RequestBody SelectOfferRequest request){
  UUID caller=customer(p);var bill=billLocked(billId);
  if(!caller.equals(bill.get("customer_id")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Not your bill");
  if(!Set.of("PENDING_APPROVAL","READY_TO_PAY").contains(bill.get("status")))throw conflict("Offer can only be selected before payment starts");
  Integer splitCount=db.queryForObject("SELECT count(*) FROM pay_bill_splits WHERE pay_bill_id=?",Integer.class,billId);
  if(splitCount!=null&&splitCount>0)throw conflict("Offers on split bills are not supported yet");
  var offers=db.queryForList("SELECT id,code,title,offer_type,cashback_percent,max_cashback_paise,min_bill_paise,eligible_bank FROM pay_bill_offers WHERE id=? AND active=true AND premium_only=false AND (restaurant_id IS NULL OR restaurant_id=?) AND starts_at<=now() AND ends_at>now() AND min_bill_paise<=?",request.offerId(),bill.get("restaurant_id"),bill.get("amount_paise"));
  if(offers.isEmpty())throw conflict("Offer is no longer eligible for this restaurant or bill amount");
  var offer=offers.get(0);
  if(!"CASHBACK".equals(offer.get("offer_type")))throw conflict("Bank discounts require payment-method verification and are not enabled yet");
  db.update("INSERT INTO pay_bill_offer_selections(pay_bill_id,offer_id,selected_at) VALUES(?,?,now()) ON CONFLICT(pay_bill_id) DO UPDATE SET offer_id=EXCLUDED.offer_id,selected_at=now()",billId,request.offerId());
  return Map.of("billId",billId,"offer",offer,"status","SELECTED");
 }
 @DeleteMapping("/customer/pay-bills/{billId}/offer") @PreAuthorize("hasRole('CUSTOMER')") @Transactional
 public Map<String,Object> removeOffer(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID billId){
  UUID caller=customer(p);var bill=billLocked(billId);
  if(!caller.equals(bill.get("customer_id")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Not your bill");
  if(!Set.of("PENDING_APPROVAL","READY_TO_PAY").contains(bill.get("status")))throw conflict("Cannot change offer after payment starts");
  db.update("DELETE FROM pay_bill_offer_selections WHERE pay_bill_id=?",billId);
  return Map.of("billId",billId,"status","REMOVED");
 }
 @GetMapping("/admin/pay-bill-offers") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
 public List<Map<String,Object>> adminOffers(){return db.queryForList("SELECT * FROM pay_bill_offers ORDER BY created_at DESC LIMIT 500");}
 @PostMapping("/admin/pay-bill-offers") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')") @Transactional
 public Map<String,Object> createOffer(@Valid @RequestBody OfferRequest o){
  if(!Set.of("CASHBACK","DISCOUNT","BANK").contains(o.offerType())||!o.endsAt().isAfter(o.startsAt()))throw conflict("Invalid offer type or validity dates");
  UUID id=UUID.randomUUID();
  db.update("INSERT INTO pay_bill_offers(id,restaurant_id,code,title,offer_type,premium_only,active,cashback_percent,max_cashback_paise,min_bill_paise,eligible_bank,starts_at,ends_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",id,o.restaurantId(),o.code(),o.title(),o.offerType(),o.premiumOnly(),o.active(),o.cashbackPercent()==null?BigDecimal.ZERO:o.cashbackPercent(),o.maxCashbackPaise(),o.minBillPaise(),o.eligibleBank(),o.startsAt(),o.endsAt());
  return db.queryForMap("SELECT * FROM pay_bill_offers WHERE id=?",id);
 }
 @GetMapping("/admin/pay-bill-adjustments") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
 public List<Map<String,Object>> adjustments(){return db.queryForList("SELECT * FROM pay_bill_adjustments ORDER BY created_at DESC LIMIT 500");}
}
