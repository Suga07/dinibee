package com.bookeat.wallet.repository;
import com.bookeat.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;
public interface WalletRepository extends JpaRepository<Wallet,UUID> {
Optional<Wallet> findByCustomerId(UUID id);
}
