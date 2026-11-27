package com.bookeat.restaurant.service;
import com.bookeat.restaurant.enums.*;
import java.time.LocalTime;
public final class WorkflowRules {
    private WorkflowRules() {}
    public static boolean ownerEditable(ApprovalStatus status) {
        return status==ApprovalStatus.DRAFT || status==ApprovalStatus.CHANGES_REQUESTED || status==ApprovalStatus.REJECTED;
    }
    public static boolean settingsEditable(ApprovalStatus status) {
        return ownerEditable(status) || status==ApprovalStatus.APPROVED;
    }
    public static boolean permits(ApprovalStatus from, ReviewAction action) {
        return switch(action) {
            case SUBMIT -> ownerEditable(from);
            case UNDER_REVIEW -> from==ApprovalStatus.SUBMITTED;
            case APPROVE -> from==ApprovalStatus.UNDER_REVIEW;
            case REJECT -> from==ApprovalStatus.SUBMITTED || from==ApprovalStatus.UNDER_REVIEW;
            case REQUEST_CHANGES -> from==ApprovalStatus.SUBMITTED || from==ApprovalStatus.UNDER_REVIEW || from==ApprovalStatus.APPROVED;
            case SUSPEND -> from==ApprovalStatus.APPROVED;
            case REACTIVATE -> from==ApprovalStatus.SUSPENDED;
            case SERVICE_CHANGE -> from==ApprovalStatus.APPROVED;
            case DOCUMENT_VERIFY, SETTLEMENT_VERIFY -> from==ApprovalStatus.UNDER_REVIEW;
        };
    }
    public static boolean validHours(boolean closed, LocalTime open, LocalTime close, boolean nextDay) {
        if(closed) return open==null && close==null && !nextDay;
        if(open==null || close==null) return false;
        return nextDay ? !close.isAfter(open) : close.isAfter(open);
    }
    public static boolean effectiveService(ApprovalStatus approval, RestaurantStatus status, boolean requested, boolean authorized, boolean enabled) {
        return approval==ApprovalStatus.APPROVED && status==RestaurantStatus.ACTIVE && requested && authorized && enabled;
    }
}
