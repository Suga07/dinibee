package com.bookeat.user.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="permissions") @Getter @Setter @NoArgsConstructor
public class Permission extends BaseEntity {
    @Column(nullable=false, unique=true, length=80) private String code;
    @Column(nullable=false, length=200) private String description;
}
