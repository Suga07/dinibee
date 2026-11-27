package com.bookeat.payment.gateway;
import java.util.*;
import java.math.BigDecimal;
public interface GatewayPort {
    boolean enabled();
    String publicKey();
    String accountId();
    boolean sandbox();
    boolean verifyCheckout(String session, String payment, String signature);
    boolean verifyWebhook(byte[] body, String signature);
    GatewayOrder create(UUID payment, BigDecimal amount);
    GatewaySession fetchSession(String sessionId);
    GatewayPayment fetchPayment(String id);
    GatewayRefund refund(String paymentId, UUID refundId, BigDecimal amount);
    GatewayRefund fetchRefund(String id);
    // Amounts passed through this port use provider-independent INR minor units.
    record GatewayOrder(String id, String receipt, long amount, String currency) {}
    record GatewaySession(GatewayOrder order, String status, List<String> paymentIds) {}
    record GatewayPayment(String id, String orderId, long amount, String currency, String status,
                          String method, long fee, long tax, long refunded) {}
    record GatewayRefund(String id, String paymentId, long amount, String status, String receipt) {}
}
