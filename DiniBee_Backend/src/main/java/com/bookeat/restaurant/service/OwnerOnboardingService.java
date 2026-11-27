package com.bookeat.restaurant.service;
import com.bookeat.restaurant.dto.RestaurantRequests.*;
import com.bookeat.restaurant.dto.RestaurantResponses.*;
import com.bookeat.common.response.PageResponse;
import java.util.UUID;
public interface OwnerOnboardingService {
    Detail create(UUID actor, CreateRestaurant request);
    PageResponse<Summary> list(UUID actor,int page,int size);
    Detail get(UUID actor,UUID id);
    Progress progress(UUID actor,UUID id);
    DashboardContext dashboard(UUID actor,UUID restaurantId);
    Detail updateDetails(UUID actor,UUID id,Details request);
    Detail updateLocation(UUID actor,UUID id,Location request);
    Detail updateBusinessInfo(UUID actor,UUID id,BusinessInfo request);
    Detail updateReservationSettings(UUID actor,UUID id,ReservationSettings request);
    Detail updateTakeawaySettings(UUID actor,UUID id,TakeawaySettings request);
    Detail updateDiniSettings(UUID actor,UUID id,DiniSettings request);
    Detail updatePolicies(UUID actor,UUID id,Policies request);
    Detail updateTableSetup(UUID actor,UUID id,TableSetup request);
    Detail updateMenuSetup(UUID actor,UUID id,MenuSetup request);
    Detail updateSettlementProfile(UUID actor,UUID id,SettlementProfile request);
    Detail updateHours(UUID actor,UUID id,Hours request);
    Detail updateSpecialSchedules(UUID actor,UUID id,SpecialSchedules request);
    Detail selectServices(UUID actor,UUID id,ServiceSelection request);
    Detail updateTeamAccess(UUID actor,UUID id,TeamAccess request);
    Detail updateForm(UUID actor,UUID id,Form request);
    Detail submit(UUID actor,UUID id,VersionRequest request);
}
