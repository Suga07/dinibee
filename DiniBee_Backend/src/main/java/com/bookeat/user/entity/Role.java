package com.bookeat.user.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Entity @Table(name="roles") @Getter @Setter @NoArgsConstructor
public class Role extends BaseEntity {
    @Enumerated(EnumType.STRING) @Column(nullable=false, unique=true, length=40)
    private RoleCode code;
    @Column(nullable=false, length=200) private String description;
    @ManyToMany
    @JoinTable(name="role_permissions", joinColumns=@JoinColumn(name="role_id"),
            inverseJoinColumns=@JoinColumn(name="permission_id"))
    private Set<Permission> permissions = new HashSet<>();
}
