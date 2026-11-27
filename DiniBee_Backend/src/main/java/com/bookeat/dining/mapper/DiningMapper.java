package com.bookeat.dining.mapper;
import com.bookeat.dining.dto.DiningResponse;import com.bookeat.dining.entity.DiningSession;import org.springframework.stereotype.Component;
@Component public class DiningMapper { public DiningResponse view(DiningSession d){return new DiningResponse(d.getId(),d.getVersion(),d.getReservationId(),d.getDiniTicketId(),d.getRestaurantId(),d.getTableId(),d.getStatus(),d.getCheckInMethod(),d.getCheckedInAt(),d.getDiningStartedAt(),d.getPaymentPendingAt(),d.getPaidAt(),d.getCheckedOutAt(),d.getBillTotal());} }
