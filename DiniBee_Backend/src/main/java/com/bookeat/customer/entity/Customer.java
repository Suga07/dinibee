package com.bookeat.customer.entity;
import com.bookeat.common.entity.BaseEntity;
import com.bookeat.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="customers") @Getter @Setter @NoArgsConstructor
public class Customer extends BaseEntity {
    @OneToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="user_id", nullable=false, unique=true)
    private User user;
}
