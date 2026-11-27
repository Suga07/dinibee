package com.bookeat.user.repository;
import com.bookeat.user.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByCode(RoleCode code);
}
