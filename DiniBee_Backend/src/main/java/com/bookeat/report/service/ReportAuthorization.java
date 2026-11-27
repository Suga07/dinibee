package com.bookeat.report.service;
import com.bookeat.report.enums.ReportType;import com.bookeat.exception.*;import org.springframework.stereotype.Component;import org.springframework.security.core.context.SecurityContextHolder;
@Component public class ReportAuthorization {
 public void check(ReportType type,boolean owner){if(owner)return;String permission=switch(type){case PAYMENTS,REFUNDS,SETTLEMENTS->"PAYMENT_VIEW";case WALLET,REWARDS->"WALLET_MANAGE";case SUPPORT->"SUPPORT_MANAGE";case CUSTOMERS->"USER_BLOCK";default->"REPORT_VIEW";};var a=SecurityContextHolder.getContext().getAuthentication();if(a==null||a.getAuthorities().stream().noneMatch(x->x.getAuthority().equals(permission)))throw new ApiException(ErrorCode.ACCESS_DENIED);}
}
