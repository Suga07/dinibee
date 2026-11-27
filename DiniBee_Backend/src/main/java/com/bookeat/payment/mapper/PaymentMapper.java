package com.bookeat.payment.mapper;
import com.bookeat.payment.dto.PaymentResponses.*;import com.bookeat.payment.entity.*;import com.bookeat.payment.gateway.GatewayPort;import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor public class PaymentMapper {
 private final GatewayPort gateway;
 public View view(Payment p){return new View(p.getId(),p.getVersion(),p.getReferenceType(),p.getReferenceId(),p.getRestaurantId(),p.getGateway(),p.getGatewayOrderId(),p.getGatewayPaymentId(),p.getAmount(),p.getDiscountAmount(),p.getCurrency(),p.getStatus(),p.getPaymentMethod(),p.getVerifiedAt(),p.getRefundedAmount(),checkout(p)==null?null:checkout(p).apiKey(),p.getDispatchStartedAt()!=null&&p.getGatewayOrderId()==null,checkout(p));}
 private Checkout checkout(Payment p){return p.getGateway()==com.bookeat.payment.enums.PaymentGateway.RAZORPAY&&gateway.enabled()&&java.util.Objects.equals(p.getGatewayAccountId(),gateway.accountId())&&java.util.Objects.equals(p.getGatewaySandbox(),gateway.sandbox())?new Checkout("RAZORPAY",p.getGatewayOrderId(),p.getGatewayAccountId(),gateway.publicKey(),"IN",gateway.sandbox()):null;}
 public RefundView refund(Refund r){return new RefundView(r.getId(),r.getVersion(),r.getPaymentId(),r.getAmount(),r.getReason(),r.getStatus(),r.getGatewayRefundId(),r.getRequestedAt(),r.getProcessedAt());}
 public CommissionView commission(CommissionRule r){return new CommissionView(r.getId(),r.getVersion(),r.getServiceType(),r.getCommissionType(),r.getCommissionValue(),r.getEffectiveFrom(),r.getEffectiveUntil(),r.isActive());}
 public SettlementView settlement(Settlement s){return new SettlementView(s.getId(),s.getVersion(),s.getRestaurantId(),s.getPeriodStart(),s.getPeriodEnd(),s.getGrossAmount(),s.getCommissionAmount(),s.getGatewayFee(),s.getTaxAmount(),s.getRefundAmount(),s.getNetAmount(),s.getStatus(),s.getSettlementReference(),s.getProcessedAt());}
}
