package com.bookeat.payment.gateway;

import com.bookeat.exception.*;
import com.bookeat.payment.service.PaymentRules;
import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.math.BigDecimal;

@Component
public class RazorpayPaymentsGateway implements GatewayPort {
    private final boolean enabled, sandbox;
    private final String keyId, keySecret, webhookSecret;
    private final ObjectMapper json;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final String BASE="https://api.razorpay.com/v1";
    public RazorpayPaymentsGateway(@Value("${app.payments.razorpay.enabled:false}") boolean enabled,
      @Value("${app.payments.razorpay.key-id:}") String keyId,
      @Value("${app.payments.razorpay.key-secret:}") String keySecret,
      @Value("${app.payments.razorpay.webhook-secret:}") String webhookSecret,ObjectMapper json){
      this.keyId=keyId.trim();this.keySecret=keySecret.trim();this.webhookSecret=webhookSecret.trim();this.json=json;
      // Local/dev convenience: supplying both Razorpay credentials is enough to enable
      // the gateway. RAZORPAY_PAYMENTS_ENABLED=true is still supported explicitly.
      boolean credentialsPresent=!this.keyId.isBlank()&&!this.keySecret.isBlank();
      this.enabled=enabled||credentialsPresent;
      this.sandbox=this.keyId.startsWith("rzp_test_");
      if(enabled&&!credentialsPresent) throw new IllegalStateException("Razorpay enabled but RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET is missing");
    }
    public boolean enabled(){return enabled;} public String publicKey(){return enabled?keyId:null;} public String accountId(){return enabled?keyId:null;} public boolean sandbox(){return sandbox;}
    public boolean verifyCheckout(String order,String payment,String signature){return enabled&&validId(order)&&validId(payment)&&constant(hmac(keySecret,order+"|"+payment),signature);}
    public boolean verifyWebhook(byte[] body,String signature){return enabled&&!webhookSecret.isBlank()&&constant(hmac(webhookSecret,new String(body,StandardCharsets.UTF_8)),signature);}
    public GatewayOrder create(UUID payment,BigDecimal amount){long minor=PaymentRules.minor(amount);var n=request("POST","/orders",Map.of("amount",minor,"currency","INR","receipt",payment.toString(),"notes",Map.of("bookeat_payment_id",payment.toString())));return new GatewayOrder(text(n,"id"),text(n,"receipt"),n.path("amount").asLong(),text(n,"currency"));}
    public GatewaySession fetchSession(String id){valid(id);var n=request("GET","/orders/"+id,null);var order=new GatewayOrder(text(n,"id"),text(n,"receipt"),n.path("amount").asLong(),text(n,"currency"));var pay=request("GET","/orders/"+id+"/payments",null);List<String> ids=new ArrayList<>();for(var x:pay.path("items"))ids.add(text(x,"id"));return new GatewaySession(order,text(n,"status"),List.copyOf(ids));}
    public GatewayPayment fetchPayment(String id){valid(id);var n=request("GET","/payments/"+id,null);long fee=n.path("fee").asLong(0),tax=n.path("tax").asLong(0),ref=n.path("amount_refunded").asLong(0);return new GatewayPayment(text(n,"id"),text(n,"order_id"),n.path("amount").asLong(),text(n,"currency"),text(n,"status"),n.path("method").asText("unknown"),fee,tax,ref);}
    public GatewayRefund refund(String paymentId,UUID refundId,BigDecimal amount){valid(paymentId);var n=request("POST","/payments/"+paymentId+"/refund",Map.of("amount",PaymentRules.minor(amount),"receipt",refundId.toString(),"notes",Map.of("bookeat_refund_id",refundId.toString())));return refund(n);}
    public GatewayRefund fetchRefund(String id){valid(id);return refund(request("GET","/refunds/"+id,null));}
    private GatewayRefund refund(JsonNode n){String receipt=n.path("receipt").asText();if(receipt.isBlank())receipt=n.path("notes").path("bookeat_refund_id").asText();return new GatewayRefund(text(n,"id"),text(n,"payment_id"),n.path("amount").asLong(),text(n,"status"),receipt);}
    private JsonNode request(String method,String path,Object body){if(!enabled)throw new ApiException(ErrorCode.PAYMENT_GATEWAY_UNAVAILABLE);try{var b=HttpRequest.newBuilder(URI.create(BASE+path)).timeout(Duration.ofSeconds(20)).header("Authorization","Basic "+Base64.getEncoder().encodeToString((keyId+":"+keySecret).getBytes(StandardCharsets.UTF_8))).header("Accept","application/json");if(body!=null)b.header("Content-Type","application/json").method(method,HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(body)));else b.method(method,HttpRequest.BodyPublishers.noBody());var r=http.send(b.build(),HttpResponse.BodyHandlers.ofByteArray());if(r.statusCode()<200||r.statusCode()>=300)throw new ApiException(ErrorCode.PAYMENT_GATEWAY_UNAVAILABLE);return json.readTree(r.body());}catch(ApiException e){throw e;}catch(Exception e){throw new ApiException(ErrorCode.PAYMENT_GATEWAY_UNAVAILABLE);}}
    private static String text(JsonNode n,String k){String v=n.path(k).asText();if(v.isBlank())throw new ApiException(ErrorCode.PAYMENT_INVALID);return v;}
    public static boolean validId(String id){return id!=null&&id.matches("[A-Za-z0-9_-]{6,100}");} private static void valid(String id){if(!validId(id))throw new ApiException(ErrorCode.PAYMENT_INVALID);}
    private static String hmac(String secret,String data){try{Mac m=Mac.getInstance("HmacSHA256");m.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(m.doFinal(data.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private static boolean constant(String a,String b){if(a==null||b==null)return false;return java.security.MessageDigest.isEqual(a.getBytes(StandardCharsets.US_ASCII),b.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));}
}
