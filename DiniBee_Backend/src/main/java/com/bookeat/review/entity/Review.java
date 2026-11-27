package com.bookeat.review.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity @Table(name="reviews") @Getter @Setter @NoArgsConstructor
public class Review extends BaseEntity {
    @Column(nullable=false) private UUID customerId;
    @Column(nullable=false) private UUID restaurantId;
    @Column(nullable=true) private UUID reservationId;
    @Column(nullable=true) private UUID foodOrderId;
    @Column(nullable=false) private int rating;
    @Column(nullable=true) private Integer foodRating;
    @Column(nullable=true) private Integer serviceRating;
    @Column(nullable=true) private Integer ambienceRating;
    @Column(nullable=true,length=3000) private String comment;
    @Column(nullable=true,length=2000) private String ownerReply;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false,length=20) private com.bookeat.review.enums.ReviewStatus status;
    @Column(nullable=true,length=1000) private String moderationReason;
}
