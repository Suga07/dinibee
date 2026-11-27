package com.bookeat.restaurant.service;
import com.bookeat.restaurant.entity.*;
import com.bookeat.restaurant.enums.*;
import com.bookeat.restaurant.repository.*;
import com.bookeat.common.audit.AuditService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.UUID;
@Service @RequiredArgsConstructor @Transactional(propagation=Propagation.MANDATORY)
public class RestaurantChangeService {
    private final OnboardingProgressService progress;
    private final RestaurantApprovalHistoryRepository history;
    private final AuditService audit;
    private final EntityManager entityManager;
    public void finish(Restaurant r,UUID actor,String action) {
        progress.refresh(r);
        r.setChangeSequence(r.getChangeSequence()+1);
        audit.record(actor,action,"Restaurant",r.getId(),null);
        entityManager.flush(); // Publish the aggregate version, including changes to child configuration rows.
    }
    public void history(Restaurant r,UUID actor,ReviewAction action,ApprovalStatus from,String reason,ServiceType service) {
        history(r,actor,action,from,reason,service,null);
    }
    public void history(Restaurant r,UUID actor,ReviewAction action,ApprovalStatus from,String reason,ServiceType service,UUID referenceId) {
        var h=new RestaurantApprovalHistory();h.setReferenceId(referenceId);h.setRestaurant(r);h.setActorId(actor);h.setAction(action);
        h.setFromStatus(from);h.setToStatus(r.getApprovalStatus());h.setReason(reason);h.setServiceType(service);history.save(h);
    }
}
