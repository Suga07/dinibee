package com.bookeat.user.entity;
import com.bookeat.common.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Entity @Table(name="users") @Getter @Setter @NoArgsConstructor
public class User extends BaseEntity {
    @Column(nullable=false, length=80) private String firstName;
    @Column(nullable=false, length=80) private String lastName;
    @Column(nullable=false, unique=true, length=320) private String email;
    @JsonIgnore @Column(nullable=false, length=100) private String passwordHash;
    @Column(length=20) private String phoneNumber;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private UserStatus status = UserStatus.ACTIVE;
    @Column(nullable=false) private boolean emailVerified;
    @Column(nullable=false) private long tokenVersion;
    @ManyToMany
    @JoinTable(name="user_roles", joinColumns=@JoinColumn(name="user_id"), inverseJoinColumns=@JoinColumn(name="role_id"))
    private Set<Role> roles = new HashSet<>();
}
