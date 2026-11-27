package com.bookeat.report.controller;

import com.bookeat.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@RestController @RequiredArgsConstructor
public class PublicPlatformStatsController {
  private final JdbcTemplate jdbc;
  @GetMapping("/api/v1/public/platform-stats")
  public ApiResponse<Map<String,Object>> stats(){
    var out=new LinkedHashMap<String,Object>();
    out.put("restaurantPartners", Optional.ofNullable(jdbc.queryForObject("select count(*) from restaurants where approval_status='APPROVED' and status='ACTIVE'",Long.class)).orElse(0L));
    out.put("dailyOrders", Optional.ofNullable(jdbc.queryForObject("select count(*) from food_orders where created_at >= date_trunc('day', now()) and created_at < date_trunc('day', now()) + interval '1 day'",Long.class)).orElse(0L));
    return ApiResponse.ok("Public platform stats loaded",out);
  }
}
