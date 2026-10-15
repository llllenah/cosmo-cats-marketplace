package com.cosmocats.marketplace.featuretoggle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class FeatureToggleAspect {

    private final FeatureToggleService featureToggleService;

    @Around("@annotation(com.cosmocats.marketplace.featuretoggle.FeatureToggle)"
            + " || @within(com.cosmocats.marketplace.featuretoggle.FeatureToggle)")
    public Object proceedIfFeatureEnabled(ProceedingJoinPoint joinPoint) throws Throwable {
        Feature feature = requiredFeature(joinPoint);
        if (!featureToggleService.isEnabled(feature)) {
            log.debug("Call to {} rejected: feature '{}' is disabled", joinPoint.getSignature().toShortString(), feature.key());
            throw new FeatureNotAvailableException(feature);
        }
        return joinPoint.proceed();
    }

    // An annotation on the method wins over the one on the class
    private static Feature requiredFeature(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        FeatureToggle toggle = AnnotatedElementUtils.findMergedAnnotation(signature.getMethod(), FeatureToggle.class);
        if (toggle == null) {
            toggle = AnnotatedElementUtils.findMergedAnnotation(joinPoint.getTarget().getClass(), FeatureToggle.class);
        }
        return toggle.value();
    }
}
