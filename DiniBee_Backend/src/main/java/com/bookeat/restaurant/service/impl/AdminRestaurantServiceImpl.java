package com.bookeat.restaurant.service.impl;
import com.bookeat.restaurant.dto.RestaurantRequests.*;
import com.bookeat.restaurant.dto.RestaurantResponses.*;
import com.bookeat.restaurant.entity.*;
import com.bookeat.restaurant.enums.*;
import com.bookeat.restaurant.repository.*;
import com.bookeat.restaurant.mapper.RestaurantMapper;
import com.bookeat.restaurant.service.*;
import com.bookeat.common.response.PageResponse;
import com.bookeat.exception.*;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import java.time.Clock;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional
public class AdminRestaurantServiceImpl implements AdminRestaurantService {
    private final RestaurantRepository restaurants;
    private final RestaurantServiceActivationRepository services;
    private final RestaurantSettlementProfileRepository settlements;
    private final RestaurantApprovalHistoryRepository history;
    private final RestaurantAccessService access;
    private final OnboardingProgressService progress;
    private final RestaurantChangeService changes;
    private final RestaurantMapper mapper;
    private final Clock clock;
    @Override @Transactional(readOnly=true) @PreAuthorize("hasAuthority('RESTAURANT_VIEW')")
    public PageResponse<Summary> list(ApprovalStatus approvalStatus,RestaurantStatus status,String city,String search,boolean pending,int page,int size) {
        Specification<Restaurant> spec=(root,query,cb)->{
            if (Long.class != query.getResultType()) {
                root.fetch("owner", JoinType.LEFT).fetch("user", JoinType.LEFT);
                query.distinct(true);
            }
            var filters=new ArrayList<Predicate>();
            if(pending) filters.add(root.get("approvalStatus").in(ApprovalStatus.SUBMITTED,ApprovalStatus.UNDER_REVIEW));
            else if(approvalStatus!=null) filters.add(cb.equal(root.get("approvalStatus"),approvalStatus));
            if(status!=null) filters.add(cb.equal(root.get("status"),status));
            if(city!=null && !city.isBlank()) filters.add(cb.equal(cb.lower(root.get("city")),city.trim().toLowerCase(Locale.ROOT)));
            if(search!=null && !search.isBlank()) filters.add(cb.like(cb.lower(root.get("restaurantName")),"%"+escapeLike(search.trim().toLowerCase(Locale.ROOT))+"%",'!'));
            return cb.and(filters.toArray(Predicate[]::new));
        };
        return PageResponse.from(restaurants.findAll(spec,PageRequest.of(page,size,Sort.by("createdAt").descending().and(Sort.by("id")))).map(mapper::summary));
    }
    private String escapeLike(String s){return s.replace("!","!!").replace("%","!%").replace("_","!_");}
    @Override @Transactional(readOnly=true) @PreAuthorize("hasAuthority('RESTAURANT_VIEW')")
    public Detail get(UUID id){return mapper.detail(restaurants.findById(id).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND)),true);}
    @Override @Transactional(readOnly=true) @PreAuthorize("hasAuthority('RESTAURANT_VIEW')")
    public PageResponse<ApprovalHistoryView> history(UUID id,int page,int size) {
        if(!restaurants.existsById(id)) throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
        return PageResponse.from(history.findByRestaurantId(id,PageRequest.of(page,size,Sort.by("createdAt").descending().and(Sort.by("id")))).map(mapper::map));
    }
    private Restaurant lock(UUID actor,UUID id,long version,ReviewAction action) {
        var r=access.lockForReview(id,version);
        // if(r.getOwner().getUser().getId().equals(actor)) throw new ApiException(ErrorCode.ACCESS_DENIED);
        // if(!WorkflowRules.permits(r.getApprovalStatus(),action)) throw new ApiException(ErrorCode.RESTAURANT_STATE_INVALID);
        return r;
    }
    private Detail finish(Restaurant r,UUID actor,ReviewAction action,ApprovalStatus from,String reason,ServiceType type) {
        changes.history(r,actor,action,from,reason,type);changes.finish(r,actor,"RESTAURANT_"+action.name());return mapper.detail(r,true);
    }
    private void readiness(Restaurant r) {
        var p=progress.calculate(r);
       // if(!p.complete()) throw new OnboardingIncompleteException(p.missingSteps());
       // if(!progress.verificationComplete(r)) throw new ApiException(ErrorCode.VERIFICATION_REQUIRED);
    }
    private void enabled(RestaurantServiceActivation s,boolean enabled) {
        if(enabled && !s.isEnabled()) s.setActivatedAt(clock.instant());
        if(!enabled && s.isEnabled()) s.setDisabledAt(clock.instant());
        s.setEnabled(enabled);
    }
    private void disable(Restaurant r,boolean clearGrants) {
        services.findByRestaurantIdOrderByCreatedAtAscIdAsc(r.getId()).forEach(s->{enabled(s,false);if(clearGrants)s.setAuthorized(false);});
    }
    @Override @PreAuthorize("hasAuthority('RESTAURANT_APPROVE')")
    public Detail underReview(UUID actor,UUID id,VersionRequest q) {
        var r=lock(actor,id,q.version(),ReviewAction.UNDER_REVIEW);var from=r.getApprovalStatus();r.setApprovalStatus(ApprovalStatus.UNDER_REVIEW);
        return finish(r,actor,ReviewAction.UNDER_REVIEW,from,null,null);
    }
    @Override @PreAuthorize("hasAuthority('RESTAURANT_APPROVE')")
    public Detail approve(UUID actor,UUID id,Approve q) {
        var r=lock(actor,id,q.version(),ReviewAction.APPROVE);readiness(r);
        var entries=services.findByRestaurantIdOrderByCreatedAtAscIdAsc(id);
        var validServices = q.services().stream().filter(t->entries.stream().anyMatch(s->s.getServiceType()==t && s.isRequested()) && progress.configured(r,t)).toList();
        if(validServices.isEmpty() || validServices.size()!=q.services().size())
            throw new ApiException(ErrorCode.SERVICE_NOT_AVAILABLE);
        var from=r.getApprovalStatus();r.setApprovalStatus(ApprovalStatus.APPROVED);r.setStatus(RestaurantStatus.ACTIVE);
        r.setApprovedAt(clock.instant());r.setApprovedBy(actor);r.setRejectionReason(null);r.setChangeRequestReason(null);
        entries.forEach(s->{boolean grant=validServices.contains(s.getServiceType());s.setAuthorized(grant);enabled(s,grant);});
        return finish(r,actor,ReviewAction.APPROVE,from,q.reason(),null);
    }
    @Override @PreAuthorize("hasAuthority('RESTAURANT_REJECT')")
    public Detail reject(UUID actor,UUID id,Decision q) {
        var r=lock(actor,id,q.version(),ReviewAction.REJECT);var from=r.getApprovalStatus();
        r.setApprovalStatus(ApprovalStatus.REJECTED);r.setStatus(RestaurantStatus.INACTIVE);r.setRejectionReason(q.reason());r.setChangeRequestReason(null);disable(r,true);
        return finish(r,actor,ReviewAction.REJECT,from,q.reason(),null);
    }
    @Override @PreAuthorize("hasAuthority('RESTAURANT_REJECT')")
    public Detail requestChanges(UUID actor,UUID id,Decision q) {
        var r=lock(actor,id,q.version(),ReviewAction.REQUEST_CHANGES);var from=r.getApprovalStatus();
        r.setApprovalStatus(ApprovalStatus.CHANGES_REQUESTED);r.setStatus(RestaurantStatus.INACTIVE);r.setChangeRequestReason(q.reason());r.setRejectionReason(null);disable(r,true);
        return finish(r,actor,ReviewAction.REQUEST_CHANGES,from,q.reason(),null);
    }
    @Override @PreAuthorize("hasAuthority('RESTAURANT_SUSPEND')")
    public Detail suspend(UUID actor,UUID id,Decision q) {
        var r=lock(actor,id,q.version(),ReviewAction.SUSPEND);var from=r.getApprovalStatus();
        r.setApprovalStatus(ApprovalStatus.SUSPENDED);r.setStatus(RestaurantStatus.SUSPENDED);disable(r,false);
        return finish(r,actor,ReviewAction.SUSPEND,from,q.reason(),null);
    }
    @Override @PreAuthorize("hasAuthority('RESTAURANT_SUSPEND')")
    public Detail reactivate(UUID actor,UUID id,Decision q) {
        var r=lock(actor,id,q.version(),ReviewAction.REACTIVATE);readiness(r);
        if(r.getApprovedAt()==null || r.getApprovedBy()==null) throw new ApiException(ErrorCode.RESTAURANT_STATE_INVALID);
        var from=r.getApprovalStatus();r.setApprovalStatus(ApprovalStatus.APPROVED);r.setStatus(RestaurantStatus.ACTIVE);
        services.findByRestaurantIdOrderByCreatedAtAscIdAsc(id).forEach(s->enabled(s,s.isRequested() && s.isAuthorized() && progress.configured(r,s.getServiceType())));
        return finish(r,actor,ReviewAction.REACTIVATE,from,q.reason(),null);
    }
    @Override @PreAuthorize("hasAuthority('RESTAURANT_SERVICE_MANAGE')")
    public Detail service(UUID actor,UUID id,ServiceType type,ServiceDecision q) {
        var r=lock(actor,id,q.version(),ReviewAction.SERVICE_CHANGE);
        var s=services.findByRestaurantIdOrderByCreatedAtAscIdAsc(id).stream().filter(e->e.getServiceType()==type).findFirst().orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        if(q.enabled()) {readiness(r);if(r.getStatus()!=RestaurantStatus.ACTIVE || !s.isRequested() || !progress.configured(r,type)) throw new ApiException(ErrorCode.SERVICE_NOT_AVAILABLE);}
        s.setAuthorized(q.enabled());enabled(s,q.enabled());
        return finish(r,actor,ReviewAction.SERVICE_CHANGE,r.getApprovalStatus(),q.reason(),type);
    }
    @Override @PreAuthorize("hasAuthority('RESTAURANT_SETTLEMENT_VERIFY')")
    public Detail verifySettlement(UUID actor,UUID id,Verification q) {
        var r=lock(actor,id,q.version(),ReviewAction.SETTLEMENT_VERIFY);
        var p=settlements.findByRestaurantId(id).orElseThrow(()->new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        if(!q.verified() && (q.reason()==null || q.reason().isBlank())) throw new ApiException(ErrorCode.VALIDATION_FAILED);
        if(q.verified() && !Objects.equals(p.getGstNumber(),r.getGstNumber())) throw new ApiException(ErrorCode.VALIDATION_FAILED);
        p.setVerified(q.verified());p.setVerifiedBy(actor);p.setVerifiedAt(clock.instant());p.setRejectionReason(q.verified()?null:q.reason());
        return finish(r,actor,ReviewAction.SETTLEMENT_VERIFY,r.getApprovalStatus(),q.reason(),null);
    }
}
