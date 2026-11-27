package com.bookeat.admin.controller;

import com.bookeat.admin.service.PromotionService;
import com.bookeat.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class CustomerOffersController {
    private final PromotionService promotions;

    @GetMapping("/api/v1/customer/offers")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<List<PromotionService.View>> activeOffers() {
        return ApiResponse.ok("Active customer offers", promotions.customerOffers());
    }
}
