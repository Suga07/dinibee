package com.bookeat.restaurant.entity;
import com.bookeat.common.entity.BaseEntity;
import com.bookeat.owner.entity.RestaurantOwner;
import com.bookeat.restaurant.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;
import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="restaurants")
public class Restaurant extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="owner_id",nullable=false)
    private RestaurantOwner owner;
    @Column(nullable=false,length=160)
    private String restaurantName;
    @Column(nullable=true,length=3000)
    private String description;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private RestaurantType restaurantType;
    @Column(nullable=false,length=320)
    private String email;
    @Column(nullable=false,length=20)
    private String phone;
    @Column(nullable=true,length=20)
    private String alternatePhone;
    @Column(nullable=true,length=250)
    private String addressLine1;
    @Column(nullable=true,length=250)
    private String addressLine2;
    @Column(nullable=true,length=100)
    private String city;
    @Column(nullable=true,length=100)
    private String state;
    @Column(nullable=true,length=20)
    private String postalCode;
    @Column(nullable=true,length=2)
    private String country;
    @Column(nullable=true,precision=10,scale=7)
    private BigDecimal latitude;
    @Column(nullable=true,precision=10,scale=7)
    private BigDecimal longitude;
    @Column(nullable=true,length=60)
    private String timezone;
    @Column(nullable=true,length=100)
    private String businessRegistrationNumber;
    @Column(nullable=true,length=30)
    private String gstNumber;
    @Column(nullable=true,length=30)
    private String fssaiNumber;
    @Column(nullable=true,length=2048)
    private String logoUrl;
    @Column(nullable=true,length=2048)
    private String coverImageUrl;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private ApprovalStatus approvalStatus;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private RestaurantStatus status;
    @Column(nullable=true)
    private Instant submittedAt;
    @Column(nullable=true)
    private Instant approvedAt;
    @Column(nullable=true)
    private UUID approvedBy;
    @Column(nullable=true,length=2000)
    private String rejectionReason;
    @Column(nullable=true,length=2000)
    private String changeRequestReason;
    @Column(nullable=false)
    private long changeSequence;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(columnDefinition="jsonb")
    private com.bookeat.restaurant.dto.OnboardingForm onboardingForm;
}
