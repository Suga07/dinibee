package com.bookeat.user.repository;
import com.bookeat.user.entity.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    List<User> findByPhoneNumber(String phoneNumber);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.email = :email")
    Optional<User> lockByEmail(@Param("email") String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.phoneNumber in :phones")
    List<User> lockByPhoneNumberIn(@Param("phones") Collection<String> phones);
    boolean existsByEmail(String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> lockById(@Param("id") UUID id);
    @Query("select (count(u) > 0) from User u join u.roles r where r.code = :code")
    boolean hasRole(@Param("code") RoleCode code);
    Page<User> findDistinctByRoles_CodeIn(Collection<RoleCode> codes, Pageable pageable);
}
