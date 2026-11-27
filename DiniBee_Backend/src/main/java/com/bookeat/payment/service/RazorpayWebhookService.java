package com.bookeat.payment.service;

import com.bookeat.exception.ApiException;
import com.bookeat.exception.ErrorCode;
import com.bookeat.membership.service.MembershipService;
import com.bookeat.payment.gateway.GatewayPort;
import com.bookeat.payment.repository.PaymentRepository;
import com.bookeat.payment.repository.RefundRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RazorpayWebhookService {
    private final GatewayPort gateway;
    private final PaymentRepository payments;
    private final RefundRepository refunds;
    private final PaymentTransactions paymentTransactions;
    private final FinanceTransactions financeTransactions;
    private final MembershipService memberships;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    @Value("${app.payments.razorpay.webhook-enabled:false}")
    private boolean webhookEnabled;

    @Transactional
    public void handle(byte[] body, String signature, String providerEventId) {
        if (!webhookEnabled || !gateway.enabled()) throw new ApiException(ErrorCode.PAYMENT_GATEWAY_UNAVAILABLE);
        if (body == null || body.length == 0 || body.length > 262_144 || !gateway.verifyWebhook(body, signature)) {
            throw new ApiException(ErrorCode.PAYMENT_INVALID);
        }

        String hash = sha256(body);
        String eventId = eventId(providerEventId, hash);
        int inserted = jdbc.update("insert into payment_webhook_events(event_id,payload_hash) values (?,?) on conflict (event_id) do nothing", eventId, hash);
        if (inserted == 0) {
            String prior = jdbc.queryForObject("select payload_hash from payment_webhook_events where event_id=?", String.class, eventId);
            if (prior == null || !MessageDigest.isEqual(prior.getBytes(StandardCharsets.US_ASCII), hash.getBytes(StandardCharsets.US_ASCII))) {
                throw new ApiException(ErrorCode.PAYMENT_INVALID);
            }
            return;
        }

        JsonNode root;
        try {
            root = json.readTree(body);
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.PAYMENT_INVALID);
        }
        String event = root.path("event").asText("").toLowerCase(Locale.ROOT);
        switch (event) {
            case "payment.captured", "order.paid" -> captured(root);
            case "payment.failed" -> failed(root);
            case "refund.processed" -> refunded(root);
            default -> { /* Signed but intentionally unsubscribed/unknown events are acknowledged. */ }
        }
    }

    private void captured(JsonNode root) {
        JsonNode entity = root.path("payload").path("payment").path("entity");
        String paymentId = entity.path("id").asText("");
        if (valid(paymentId)) {
            captureProof(gateway.fetchPayment(paymentId));
            return;
        }
        String orderId = root.path("payload").path("order").path("entity").path("id").asText("");
        if (!valid(orderId)) throw new ApiException(ErrorCode.PAYMENT_INVALID);
        var session = gateway.fetchSession(orderId);
        for (String candidate : session.paymentIds()) {
            var proof = gateway.fetchPayment(candidate);
            if ("captured".equals(proof.status())) {
                captureProof(proof);
                return;
            }
        }
        throw new ApiException(ErrorCode.PAYMENT_INVALID);
    }

    private void captureProof(GatewayPort.GatewayPayment proof) {
        var payment = payments.findByGatewayOrderId(proof.orderId());
        if (payment.isPresent()) {
            paymentTransactions.capture(payment.get().getId(), proof, null);
            return;
        }
        // Pay Bill uses the same signed Razorpay webhook (no second webhook URL).
        // Never trust the webhook payload alone: proof was fetched directly from Razorpay.
        var bill = jdbc.query("SELECT id, amount_paise, status FROM pay_bills WHERE razorpay_order_id=? FOR UPDATE",
            rs -> rs.next() ? new Object[]{rs.getObject(1,UUID.class),rs.getLong(2),rs.getString(3)} : null, proof.orderId());
        if (bill != null) {
            if (!"captured".equalsIgnoreCase(proof.status()) || !"INR".equals(proof.currency()) || ((Long)bill[1]).longValue()!=proof.amount()) {
                throw new ApiException(ErrorCode.PAYMENT_INVALID);
            }
            UUID billId=(UUID)bill[0];
            jdbc.update("UPDATE pay_bill_payments SET status='PAID',razorpay_payment_id=?,verified_at=now() WHERE pay_bill_id=? AND status='PENDING'",proof.id(),billId);
            jdbc.update("UPDATE pay_bills SET status='PAID',razorpay_payment_id=?,updated_at=now() WHERE id=? AND status='PAYMENT_PENDING'",proof.id(),billId);
            return;
        }
        memberships.webhookCaptured(proof); // Unknown orders from the same Razorpay account are safely ignored.
    }

    private void failed(JsonNode root) {
        String paymentId = root.path("payload").path("payment").path("entity").path("id").asText("");
        if (!valid(paymentId)) throw new ApiException(ErrorCode.PAYMENT_INVALID);
        var proof = gateway.fetchPayment(paymentId);
        if (!"failed".equals(proof.status())) throw new ApiException(ErrorCode.PAYMENT_INVALID);
        paymentTransactions.gatewayFailure(proof);
    }

    private void refunded(JsonNode root) {
        String refundId = root.path("payload").path("refund").path("entity").path("id").asText("");
        if (!valid(refundId)) throw new ApiException(ErrorCode.PAYMENT_INVALID);
        var proof = gateway.fetchRefund(refundId);
        var local = refunds.findByGatewayRefundId(proof.id());
        if (local.isEmpty() && proof.receipt() != null) {
            try { local = refunds.findById(UUID.fromString(proof.receipt())); }
            catch (IllegalArgumentException ignored) { /* Not one of DiniBee's refunds. */ }
        }
        if (local.isPresent()) financeTransactions.attachRefund(null, local.get().getId(), proof);
    }

    private static boolean valid(String id) {
        return com.bookeat.payment.gateway.RazorpayPaymentsGateway.validId(id);
    }

    private static String eventId(String raw, String hash) {
        if (raw == null || raw.isBlank()) return "body_" + hash;
        String id = raw.trim();
        if (id.length() > 150 || !id.matches("[A-Za-z0-9_-]{6,150}")) throw new ApiException(ErrorCode.PAYMENT_INVALID);
        return id;
    }

    private static String sha256(byte[] body) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body)); }
        catch (Exception ex) { throw new IllegalStateException("SHA-256 unavailable", ex); }
    }
}
