package com.bookeat.admin.repository;
import com.bookeat.admin.entity.Promotion;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
import java.time.*;
public interface PromotionRepository extends JpaRepository<Promotion,UUID> {

Page<Promotion> findByKind(String kind,Pageable p);
Optional<Promotion> findByCode(String code);
@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select p from Promotion p where p.code=:code") Optional<Promotion> lockByCode(@Param("code") String code);
}
