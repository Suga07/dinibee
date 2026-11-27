package com.bookeat.payment.service;
import com.bookeat.payment.dto.*;import com.bookeat.payment.repository.*;import com.bookeat.payment.mapper.PaymentMapper;import com.bookeat.payment.gateway.GatewayPort;import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import java.util.UUID;import com.bookeat.exception.*;
@Service @RequiredArgsConstructor public class FinanceServiceImpl implements FinanceService {
 private final FinanceTransactions transactions;private final RefundRepository refunds;private final PaymentRepository payments;private final SettlementRepository settlements;private final PaymentMapper mapper;private final GatewayPort gateway;
 public PaymentResponses.RefundView requestRefund(UUID actor,String key,PaymentRequests.RefundRequest q){return mapper.refund(refunds.findById(transactions.request(actor,key,q)).orElseThrow());}
 public PaymentResponses.RefundView approve(UUID actor,UUID id,PaymentRequests.Action q,boolean approve){return mapper.refund(transactions.decide(actor,id,q,approve));}
 public PaymentResponses.RefundView process(UUID actor,UUID id){if(transactions.dispatch(actor,id)){var r=refunds.findById(id).orElseThrow();var p=payments.findById(r.getPaymentId()).orElseThrow();try{transactions.attachRefund(actor,id,gateway.refund(p.getGatewayPaymentId(),id,r.getAmount()));}catch(ApiException ex){/* Durable PROCESSING state prevents a blind repeat after an uncertain provider outcome. */}}return mapper.refund(refunds.findById(id).orElseThrow());}
 public PaymentResponses.RefundView reconcileRefund(UUID actor,UUID id,String proofId){transactions.attachRefund(actor,id,gateway.fetchRefund(proofId));return mapper.refund(refunds.findById(id).orElseThrow());}
 public PaymentResponses.SettlementView createSettlement(UUID actor,String key,PaymentRequests.SettlementCreate q){return mapper.settlement(settlements.findById(transactions.settlement(actor,key,q)).orElseThrow());}
 public PaymentResponses.SettlementView settlementAction(UUID actor,UUID id,PaymentRequests.SettlementAction q){return mapper.settlement(transactions.settlementAction(actor,id,q));}
}
