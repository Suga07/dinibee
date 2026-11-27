package com.bookeat.restaurant.controller;
import com.bookeat.restaurant.dto.RestaurantRequests.*;
import com.bookeat.restaurant.dto.RestaurantResponses.*;
import com.bookeat.restaurant.enums.*;
import com.bookeat.restaurant.service.*;
import com.bookeat.common.response.ApiResponse;
import com.bookeat.common.response.PageResponse;
import com.bookeat.security.AppPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/owner")
@SecurityRequirement(name="bearerAuth") @Tag(name="Owner dashboard")
@io.swagger.v3.oas.annotations.responses.ApiResponses({
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400",description="Invalid input; inspect ApiResponse.errors"),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401",description="Bearer token absent or expired"),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403",description="Required role or permission is missing"),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="404",description="Restaurant/document absent or not owned"),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="409",description="Stale version, invalid state, or required verification missing"),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="413",description="Document exceeds the upload size limit"),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="422",description="Required onboarding steps incomplete"),
 @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="503",description="Storage or settlement encryption unavailable")
})
public class OwnerDashboardController {
    private final OwnerOnboardingService onboarding;
    @GetMapping("/dashboard-context") @Operation(summary="Load owner dashboard context",description="OWNER + RESTAURANT_OWN_MANAGE. Optional restaurantId selects an owned restaurant; otherwise the oldest is selected. No restaurant yields empty enabledServices and onboardingComplete=false. Only APPROVED + ACTIVE authorized services are enabled.")
    public ApiResponse<DashboardContext> dashboard(@AuthenticationPrincipal AppPrincipal p,@RequestParam(required=false) UUID restaurantId){return ApiResponse.ok("Dashboard context loaded",onboarding.dashboard(p.userId(),restaurantId));}
}
