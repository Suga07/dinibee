package com.bookeat.report.service;
import com.bookeat.report.dto.ReportQuery;import com.bookeat.report.enums.ReportType;import com.bookeat.common.response.PageResponse;import java.util.*;
public interface ReportService {PageResponse<Map<String,Object>> report(UUID actor,UUID ownedRestaurant,ReportType type,ReportQuery query);Map<String,Object> dashboard();byte[] export(UUID actor,UUID ownedRestaurant,ReportType type,ReportQuery query,String format);}
