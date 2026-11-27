package com.bookeat.payment.service;
import com.bookeat.payment.dto.*;import com.bookeat.common.response.PageResponse;import java.util.UUID;
public interface PaymentService {
 PaymentResponses.View create(UUID actor,String key,PaymentRequests.Create request);PaymentResponses.View verify(UUID actor,PaymentRequests.Verify request);PaymentResponses.View get(UUID actor,UUID id);PageResponse<PaymentResponses.View> list(UUID actor,int page,int size);PaymentResponses.View reconcile(UUID id,String gatewaySessionId);PaymentResponses.View refresh(UUID actor,UUID id);PaymentResponses.View cash(UUID actor,UUID restaurant,UUID id,PaymentRequests.Action request);
}
