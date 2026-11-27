package com.bookeat.wallet.repository;
import com.bookeat.wallet.entity.WalletTransaction;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction,UUID> {
Page<WalletTransaction> findByWalletId(UUID id,Pageable p);
}
