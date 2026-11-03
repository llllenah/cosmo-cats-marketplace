package com.cosmocats.marketplace.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Checks that order items do not list the same product twice: the quantity is set in one item.
 * Null lists and items without productId are left to @NotEmpty and @NotNull.
 */
@Documented
@Constraint(validatedBy = EachProductOnceValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface EachProductOnce {

    String message() default "must not contain the same product twice";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
