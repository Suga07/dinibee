package com.bookeat.payment.controller;

import com.bookeat.payment.service.RazorpayWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RazorpayWebhookController {
    private final RazorpayWebhookService webhooks;

    @PostMapping(value = "/api/v1/payments/razorpay/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> receive(
            @RequestHeader("X-Razorpay-Signature") String signature,
            @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId,
            @RequestBody byte[] body) {
        webhooks.handle(body, signature, eventId);
        return ResponseEntity.ok().build();
    }
}
