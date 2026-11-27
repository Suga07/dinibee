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
import java.sql.ResultSet;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PayBillController {
  private final JdbcTemplate db;
  private final RazorpayPaymentsGateway razorpay;

  public record BillRequest(@NotNull UUID restaurantId, UUID customerId,
      @NotBlank @Size(max=90) String billNumber, @NotNull @Min(1) Long amountPaise) {}
  public record Decision(@Size(max=500) String reason) {}
  public record VerifyRequest(@NotBlank String paymentId, @NotBlank String signature) {}
  private ResponseStatusException invalid(String message) {return new ResponseStatusException(HttpStatus.CONFLICT,message);}
  private void owner(UUID user,UUID restaurant) {
    Integer count=db.queryForObject("SELECT count(*) FROM restaurants r JOIN restaurant_owners o ON o.id=r.owner_id WHERE r.id=? AND o.user_id=?",Integer.class,restaurant,user);
    if(count==null||count==0) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Not your restaurant");
  }
  private void customer(UUID user,UUID customer) {
    Integer count=db.queryForObject("SELECT count(*) FROM customers WHERE id=? AND user_id=?",Integer.class,customer,user);
    if(count==null||count==0) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Not your bill");
  }
  private Map<String,Object> bill(UUID id) {
    return db.query("SELECT * FROM pay_bills WHERE id=?",rs -> {
      if(!rs.next())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Bill not found");
      Map<String,Object> row=new LinkedHashMap<>();
      for(String col:List.of("id","restaurant_id","customer_id","bill_number","source","amount_paise","currency","status","razorpay_order_id"))row.put(col,rs.getObject(col));
      return row;
    },id);
  }
  @PostMapping("/owner/restaurants/{restaurantId}/pay-bills")
  @PreAuthorize("hasRole('OWNER')") @Transactional
  public Map<String,Object> create(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@Valid @RequestBody BillRequest q){
    owner(p.userId(),restaurantId);
    if(!restaurantId.equals(q.restaurantId()))throw invalid("Restaurant mismatch");
    if(q.customerId()!=null) {
      Integer present=db.queryForObject("SELECT count(*) FROM customers WHERE id=?",Integer.class,q.customerId());
      if(present==null||present==0)throw invalid("Customer not found");
    }
    UUID id=UUID.randomUUID();
    db.update("INSERT INTO pay_bills(id,restaurant_id,customer_id,bill_number,source,amount_paise,status) VALUES(?,?,?,?,?,?,?)",id,restaurantId,q.customerId(),q.billNumber(),"RESTAURANT",q.amountPaise(),"READY_TO_PAY");
    return bill(id);
  }
  @PostMapping("/customer/pay-bills/manual")
  @PreAuthorize("hasRole('CUSTOMER')") @Transactional
  public Map<String,Object> manual(@AuthenticationPrincipal AppPrincipal p,@Valid @RequestBody BillRequest q){
    UUID customer=db.query("SELECT id FROM customers WHERE user_id=?",rs -> rs.next()?rs.getObject(1,UUID.class):null,p.userId());
    if(customer==null)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Customer unavailable");
    UUID id=UUID.randomUUID();
    db.update("INSERT INTO pay_bills(id,restaurant_id,customer_id,bill_number,source,amount_paise,status) VALUES(?,?,?,?,?,?,?)",id,q.restaurantId(),customer,q.billNumber(),"MANUAL",q.amountPaise(),"PENDING_APPROVAL");
    return bill(id);
  }
  @GetMapping("/admin/pay-bills") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
  public List<Map<String,Object>> adminBills(){
    return db.queryForList("SELECT id,restaurant_id,customer_id,bill_number,source,amount_paise,status,razorpay_order_id,created_at FROM pay_bills ORDER BY created_at DESC LIMIT 500");
  }
  @GetMapping("/customer/pay-bills") @PreAuthorize("hasRole('CUSTOMER')")
  public List<Map<String,Object>> mine(@AuthenticationPrincipal AppPrincipal p){
    return db.queryForList("SELECT b.id,b.restaurant_id,b.bill_number,b.source,b.amount_paise,b.status,b.created_at FROM pay_bills b JOIN customers c ON c.id=b.customer_id WHERE c.user_id=? ORDER BY b.created_at DESC LIMIT 100",p.userId());
  }
  @GetMapping("/owner/restaurants/{restaurantId}/pay-bills") @PreAuthorize("hasRole('OWNER')")
  public List<Map<String,Object>> restaurantBills(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId){
    owner(p.userId(),restaurantId);return db.queryForList("SELECT id,customer_id,bill_number,source,amount_paise,status,created_at FROM pay_bills WHERE restaurant_id=? ORDER BY created_at DESC LIMIT 100",restaurantId);
  }
  @PostMapping("/owner/restaurants/{restaurantId}/pay-bills/{id}/verify") @PreAuthorize("hasRole('OWNER')") @Transactional
  public Map<String,Object> approve(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id){
    owner(p.userId(),restaurantId);
    int updated=db.update("UPDATE pay_bills SET status='READY_TO_PAY',verified_by=?,verified_at=now(),updated_at=now() WHERE id=? AND restaurant_id=? AND source='MANUAL' AND status='PENDING_APPROVAL'",p.userId(),id,restaurantId);
    if(updated!=1)throw invalid("Bill cannot be approved in its current state");return bill(id);
  }
  @PostMapping("/owner/restaurants/{restaurantId}/pay-bills/{id}/reject") @PreAuthorize("hasRole('OWNER')") @Transactional
  public Map<String,Object> reject(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID restaurantId,@PathVariable UUID id,@Valid @RequestBody Decision q){
    owner(p.userId(),restaurantId);
    int updated=db.update("UPDATE pay_bills SET status='REJECTED',rejection_reason=?,updated_at=now() WHERE id=? AND restaurant_id=? AND status='PENDING_APPROVAL'",q.reason(),id,restaurantId);
    if(updated!=1)throw invalid("Bill cannot be rejected in its current state");return bill(id);
  }
  @PostMapping("/customer/pay-bills/{id}/payment-order") @PreAuthorize("hasRole('CUSTOMER')") @Transactional
  public Map<String,Object> order(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id){
    // Serialize concurrent order creation for this bill; prevents duplicate charges.
    db.queryForObject("SELECT id FROM pay_bills WHERE id=? FOR UPDATE", UUID.class, id);
    var b=bill(id);UUID customerId=(UUID)b.get("customer_id");
    if(customerId==null)throw invalid("Restaurant must assign the bill to a customer");customer(p.userId(),customerId);
    String status=(String)b.get("status");
    if("PAID".equals(status))throw invalid("Bill already paid");
    Integer splitCount=db.queryForObject("SELECT count(*) FROM pay_bill_splits WHERE pay_bill_id=?",Integer.class,id);
    if(splitCount!=null&&splitCount>0)throw invalid("This bill is split; guests must pay their individual shares");
    if(!"READY_TO_PAY".equals(status)&&!"PAYMENT_PENDING".equals(status))throw invalid("Restaurant approval is required");
    if(b.get("razorpay_order_id")!=null)return Map.of("billId",id,"orderId",b.get("razorpay_order_id"),"key",razorpay.publicKey(),"amountPaise",b.get("amount_paise"));
    long minor=((Number)b.get("amount_paise")).longValue();
    GatewayOrder order=razorpay.create(id,BigDecimal.valueOf(minor,2));
    int updated=db.update("UPDATE pay_bills SET razorpay_order_id=?,status='PAYMENT_PENDING',updated_at=now() WHERE id=? AND razorpay_order_id IS NULL AND status='READY_TO_PAY'",order.id(),id);
    if(updated!=1)throw invalid("Payment already being created; refresh bill");
    db.update("INSERT INTO pay_bill_payments(id,pay_bill_id,razorpay_order_id,amount_paise,status) VALUES(?,?,?,?,?)",UUID.randomUUID(),id,order.id(),minor,"PENDING");
    return Map.of("billId",id,"orderId",order.id(),"key",razorpay.publicKey(),"amountPaise",minor);
  }
  @PostMapping("/customer/pay-bills/{id}/verify-payment") @PreAuthorize("hasRole('CUSTOMER')") @Transactional
  public Map<String,Object> verifyPayment(@AuthenticationPrincipal AppPrincipal p,@PathVariable UUID id,@Valid @RequestBody VerifyRequest req){
    var b=bill(id);UUID customerId=(UUID)b.get("customer_id");
    if(customerId==null)throw invalid("Customer not assigned");customer(p.userId(),customerId);
    String orderId=(String)b.get("razorpay_order_id");if(orderId==null)throw invalid("Order not created");
    if(!razorpay.verifyCheckout(orderId,req.paymentId(),req.signature()))throw invalid("Invalid payment signature");
    var payment=razorpay.fetchPayment(req.paymentId());
    if(!orderId.equals(payment.orderId())||!"captured".equalsIgnoreCase(payment.status())||!"INR".equals(payment.currency())||payment.amount()!=((Number)b.get("amount_paise")).longValue())throw invalid("Gateway payment is not captured for this amount");
    db.update("UPDATE pay_bill_payments SET status='PAID',razorpay_payment_id=?,verified_at=now() WHERE razorpay_order_id=? AND status='PENDING'",req.paymentId(),orderId);
    db.update("UPDATE pay_bills SET status='PAID',razorpay_payment_id=?,updated_at=now() WHERE id=? AND razorpay_order_id=? AND status='PAYMENT_PENDING'",req.paymentId(),id,orderId);
    return bill(id);
  }
}
