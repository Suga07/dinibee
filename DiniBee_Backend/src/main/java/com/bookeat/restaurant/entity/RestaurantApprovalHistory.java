package com.bookeat.restaurant.entity;
import com.bookeat.common.entity.BaseEntity;
import com.bookeat.owner.entity.RestaurantOwner;
import com.bookeat.restaurant.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;
import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="restaurant_approval_history")
public class RestaurantApprovalHistory extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="restaurant_id",nullable=false)
    private Restaurant restaurant;
    @Column(nullable=false)
    private UUID actorId;
    @Column private UUID referenceId;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private ReviewAction action;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private ApprovalStatus fromStatus;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private ApprovalStatus toStatus;
    @Column(nullable=true,length=2000)
    private String reason;
    @Enumerated(EnumType.STRING) @Column(nullable=true)
    private ServiceType serviceType;
}
