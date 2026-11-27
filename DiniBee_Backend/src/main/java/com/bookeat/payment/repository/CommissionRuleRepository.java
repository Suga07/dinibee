package com.bookeat.payment.repository;
import com.bookeat.payment.entity.CommissionRule;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;
public interface CommissionRuleRepository extends JpaRepository<CommissionRule,UUID> {

}
