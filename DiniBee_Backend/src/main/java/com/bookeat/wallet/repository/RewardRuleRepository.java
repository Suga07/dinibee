package com.bookeat.wallet.repository;
import com.bookeat.wallet.entity.RewardRule;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;
public interface RewardRuleRepository extends JpaRepository<RewardRule,UUID> {

}
