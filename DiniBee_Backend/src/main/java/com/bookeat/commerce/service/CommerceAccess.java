package com.bookeat.commerce.service;
import com.bookeat.customer.repository.CustomerRepository;
import com.bookeat.restaurant.entity.Restaurant;
import com.bookeat.restaurant.entity.DiniTicketSettings;
import com.bookeat.restaurant.enums.*;
import com.bookeat.restaurant.repository.*;
import com.bookeat.restaurant.service.WorkflowRules;
import com.bookeat.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.RequiredArgsConstructor;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor
public class CommerceAccess {
    private final CustomerRepository customers;
    private final RestaurantRepository restaurants;
    private final RestaurantServiceActivationRepository activations;
    private final DiniTicketSettingsRepository diniTicketSettings;
    private final JdbcTemplate jdbc;
    @Transactional(readOnly=true) public UUID customer(UUID actor){return customers.findByUserId(actor).orElseThrow(()->new ApiException(ErrorCode.ACCESS_DENIED)).getId();}
    @Transactional(propagation=Propagation.MANDATORY) public Restaurant lock(UUID restaurantId){return restaurants.lockById(restaurantId).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));}
    @Transactional(propagation=Propagation.MANDATORY) public Restaurant lockOwner(UUID actor,UUID restaurantId){return restaurants.lockOwned(restaurantId,actor).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));}
    @Transactional(readOnly=true) public Restaurant publicRestaurant(UUID id,ServiceType service){var r=restaurants.findById(id).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));requireService(r,service);return r;}
    public void requireService(Restaurant r,ServiceType service){
        if(enabled(r).contains(service)) return;
        // Compatibility repair for approved restaurants whose DiniTicket settings
        // are enabled but whose legacy service-activation row was not promoted.
        // Owner DiniTicket settings are the source of truth for runtime availability;
        // other services continue to use the strict activation workflow.
        if(service==ServiceType.DINITICKET
                && r.getApprovalStatus()==ApprovalStatus.APPROVED
                && r.getStatus()==RestaurantStatus.ACTIVE
                && diniTicketSettings.findByRestaurantId(r.getId()).map(DiniTicketSettings::isEnabled).orElse(false)) {
            return;
        }
        throw new ApiException(ErrorCode.SERVICE_NOT_AVAILABLE);
    }
    public Set<ServiceType> enabled(Restaurant r){var result=EnumSet.noneOf(ServiceType.class);for(var a:activations.findByRestaurantIdOrderByCreatedAtAscIdAsc(r.getId()))if(WorkflowRules.effectiveService(r.getApprovalStatus(),r.getStatus(),a.isRequested(),a.isAuthorized(),a.isEnabled()))result.add(a.getServiceType());return result;}
    public boolean openAt(UUID id,Instant instant){return Boolean.TRUE.equals(jdbc.queryForObject("select restaurant_open_at(?,?)",Boolean.class,id,java.sql.Timestamp.from(instant)));}
    public void version(long actual,long expected){if(actual!=expected)throw new ApiException(ErrorCode.CONFLICT);}
    public void require(boolean condition){if(!condition)throw new ApiException(ErrorCode.VALIDATION_FAILED);}
}
