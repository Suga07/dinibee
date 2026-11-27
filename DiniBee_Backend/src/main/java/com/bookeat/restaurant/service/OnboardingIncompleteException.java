package com.bookeat.restaurant.service;
import com.bookeat.exception.*;
import com.bookeat.restaurant.enums.OnboardingStep;
import java.util.List;
public class OnboardingIncompleteException extends ApiException {
    private final List<OnboardingStep> steps;
    public OnboardingIncompleteException(List<OnboardingStep> steps){super(ErrorCode.ONBOARDING_INCOMPLETE);this.steps=List.copyOf(steps);}
    public List<OnboardingStep> getSteps(){return steps;}
}
