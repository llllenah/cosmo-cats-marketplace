package com.cosmocats.marketplace.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Checks that a product name contains at least one cosmic term, e.g. "star", "galaxy" or "comet".
 * Null values are considered valid, use @NotBlank to forbid them.
 */
@Documented
@Constraint(validatedBy = CosmicWordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface CosmicWordCheck {

    String message() default "must contain at least one cosmic word (star, galaxy, comet, cosmic, space, planet, moon, nebula, orbit, asteroid)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
