package com.bookeat.report.dto;
import jakarta.validation.constraints.*;import java.time.LocalDate;import java.util.UUID;import com.bookeat.restaurant.enums.ServiceType;
public record ReportQuery(@NotNull LocalDate fromDate,@NotNull LocalDate toDate,UUID restaurantId,@Size(max=30) String status,ServiceType serviceType,@Size(max=100) String city,@Min(0) @Max(10000) Integer page,@Min(1) @Max(100) Integer size){}
