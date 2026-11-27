package com.bookeat.payment.service;
import com.bookeat.payment.dto.*;import com.bookeat.common.response.PageResponse;import java.util.UUID;
public interface FinanceService {
 PaymentResponses.RefundView requestRefund(UUID actor,String key,PaymentRequests.RefundRequest request);
 PaymentResponses.RefundView approve(UUID actor,UUID id,PaymentRequests.Action request,boolean approve);
 PaymentResponses.RefundView process(UUID actor,UUID id);PaymentResponses.RefundView reconcileRefund(UUID actor,UUID id,String gatewayRefundId);
 PaymentResponses.SettlementView createSettlement(UUID actor,String key,PaymentRequests.SettlementCreate request);
 PaymentResponses.SettlementView settlementAction(UUID actor,UUID id,PaymentRequests.SettlementAction request);
}
