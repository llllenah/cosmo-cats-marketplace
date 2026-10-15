package com.cosmocats.marketplace.featuretoggle;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The method (or every public method of the class) runs only when the feature is enabled,
 * otherwise {@link FeatureNotAvailableException} is thrown. Checked by {@link FeatureToggleAspect}.
 * <p>
 * Works through a Spring proxy, so a call from another method of the same class is not checked.
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface FeatureToggle {

    Feature value();
}
