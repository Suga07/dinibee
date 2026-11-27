package com.bookeat.admin.controller;

import com.bookeat.common.response.ApiResponse;
import com.bookeat.common.response.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/customers")
public class AdminCustomerController {
    private final NamedParameterJdbcTemplate jdbc;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OPERATIONS_ADMIN','SUPPORT_ADMIN') or hasAuthority('USER_BLOCK')")
    public ApiResponse<PageResponse<Map<String,Object>>> list(
            @RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        var p = new MapSqlParameterSource().addValue("limit", size).addValue("offset", (long) page * size);
        long total = Optional.ofNullable(jdbc.queryForObject("select count(*) from customers", new MapSqlParameterSource(), Long.class)).orElse(0L);
        String sql = """
            select u.id, u.first_name as \"firstName\", u.last_name as \"lastName\", u.email,
                   u.phone_number as \"phoneNumber\", u.status, u.created_at as \"createdAt\",
                   (select count(*) from reservations r where r.customer_id=c.id) as \"reservationCount\",
                   (select count(*) from takeaway_orders t where t.customer_id=c.id) as \"takeawayCount\",
                   coalesce((select sum(p.amount-p.refunded_amount) from payments p where p.customer_id=c.id and p.status in ('SUCCESS','PARTIALLY_REFUNDED','REFUNDED')),0) as \"totalSpending\"
            from customers c join users u on u.id=c.user_id
            order by u.created_at desc, u.id desc limit :limit offset :offset
            """;
        var rows = jdbc.queryForList(sql, p);
        int totalPages = (int)((total + size - 1) / size);
        return ApiResponse.ok("Customers loaded", new PageResponse<>(rows,page,size,total,totalPages,page==0,(long)(page+1)*size>=total));
    }
}
