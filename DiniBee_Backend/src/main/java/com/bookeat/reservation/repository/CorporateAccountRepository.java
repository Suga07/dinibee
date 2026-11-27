package com.bookeat.reservation.repository;
import com.bookeat.reservation.entity.CorporateAccount;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;
public interface CorporateAccountRepository extends JpaRepository<CorporateAccount,UUID> {
    Optional<CorporateAccount> findByIdAndOwnerCustomerId(UUID id,UUID customerId);
}
