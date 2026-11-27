package com.bookeat.payment.controller;

import com.bookeat.common.response.ApiResponse;
import com.bookeat.payment.gateway.GatewayPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PaymentMethodOptionsController {
    private final GatewayPort razorpay;

    public record PaymentMethodOption(
            String code,
            String title,
            String description,
            String gateway,
            boolean enabled) {}

    @GetMapping("/api/v1/customer/payment-methods/options")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "List payment methods currently available to the customer")
    public ApiResponse<List<PaymentMethodOption>> options() {
        boolean online = razorpay.enabled();
        return ApiResponse.ok("Payment methods loaded", List.of(
                new PaymentMethodOption("UPI", "UPI", "Pay using a UPI app through Razorpay", "RAZORPAY", online),
                new PaymentMethodOption("CARD", "Credit / Debit Card", "Pay securely by card through Razorpay", "RAZORPAY", online),
                new PaymentMethodOption("NET_BANKING", "Net Banking", "Choose your bank in Razorpay checkout", "RAZORPAY", online),
                new PaymentMethodOption("COD", "Cash / Pay at Restaurant", "Available when the restaurant allows cash payment", "CASH", true)
        ));
    }
}
