package com.bookeat.restaurant.entity;
import com.bookeat.common.entity.BaseEntity;
import com.bookeat.owner.entity.RestaurantOwner;
import com.bookeat.restaurant.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;
import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor @Entity @Table(name="restaurant_documents")
public class RestaurantDocument extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="restaurant_id",nullable=false)
    private Restaurant restaurant;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private DocumentType documentType;
    @Column(nullable=false,length=100)
    private String documentNumber;
    @Column(nullable=false,length=2048)
    private String fileUrl;
    @com.fasterxml.jackson.annotation.JsonIgnore @Column(nullable=false,length=250)
    private String fileKey;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private StorageProvider storageProvider;
    @Column(nullable=false,length=150)
    private String originalFilename;
    @Column(nullable=false,length=100)
    private String contentType;
    @Column(nullable=false)
    private long sizeBytes;
    @Column(nullable=false,length=64)
    private String sha256;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private VerificationStatus verificationStatus;
    @Column(nullable=true)
    private UUID verifiedBy;
    @Column(nullable=true)
    private Instant verifiedAt;
    @Column(nullable=true,length=2000)
    private String rejectionReason;
    @Column(nullable=true)
    private Instant deletedAt;
}
