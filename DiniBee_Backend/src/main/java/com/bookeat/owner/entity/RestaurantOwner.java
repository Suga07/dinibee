package com.bookeat.owner.entity;
import com.bookeat.common.entity.BaseEntity;
import com.bookeat.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="restaurant_owners") @Getter @Setter @NoArgsConstructor
public class RestaurantOwner extends BaseEntity {
    @OneToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="user_id", nullable=false, unique=true)
    private User user;
}
