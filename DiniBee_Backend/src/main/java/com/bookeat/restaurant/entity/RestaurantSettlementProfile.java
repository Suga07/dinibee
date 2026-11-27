package com.bookeat.restaurant.entity;
import com.bookeat.common.entity.BaseEntity;
import com.bookeat.owner.entity.RestaurantOwner;
import com.bookeat.restaurant.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;
import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="restaurant_settlement_profiles")
public class RestaurantSettlementProfile extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="restaurant_id",nullable=false,unique=true)
    private Restaurant restaurant;
    @Column(nullable=false,length=160)
    private String accountHolderName;
    @Column(nullable=false,length=160)
    private String bankName;
    @com.fasterxml.jackson.annotation.JsonIgnore @Column(nullable=false,length=500)
    private String encryptedAccountNumber;
    @Column(nullable=false,length=4)
    private String accountNumberLast4;
    @Column(nullable=false,length=11)
    private String ifscCode;
    @Column(nullable=true,length=150)
    private String upiId;
    @Column(nullable=false,length=160)
    private String taxName;
    @Column(nullable=true,length=30)
    private String gstNumber;
    @Column(nullable=false)
    private boolean verified;
    @Column(nullable=true)
    private UUID verifiedBy;
    @Column(nullable=true)
    private Instant verifiedAt;
    @Column(nullable=true,length=2000)
    private String rejectionReason;
}
