package com.bookeat.restaurant.service;
import com.bookeat.restaurant.dto.RestaurantRequests.*;
import com.bookeat.restaurant.dto.RestaurantResponses.*;
import com.bookeat.restaurant.enums.*;
import com.bookeat.common.response.PageResponse;
import java.util.UUID;
public interface AdminRestaurantService {
    PageResponse<Summary> list(ApprovalStatus approvalStatus,RestaurantStatus status,String city,String search,boolean pending,int page,int size);
    Detail get(UUID id);
    PageResponse<ApprovalHistoryView> history(UUID id,int page,int size);
    Detail underReview(UUID actor,UUID id,VersionRequest request);
    Detail approve(UUID actor,UUID id,Approve request);
    Detail reject(UUID actor,UUID id,Decision request);
    Detail requestChanges(UUID actor,UUID id,Decision request);
    Detail suspend(UUID actor,UUID id,Decision request);
    Detail reactivate(UUID actor,UUID id,Decision request);
    Detail service(UUID actor,UUID id,ServiceType type,ServiceDecision request);
    Detail verifySettlement(UUID actor,UUID id,Verification request);
}
