package com.cosmocats.marketplace.application.exception;

import java.util.List;

/**
 * Thrown when product data breaks a business rule detected outside Bean Validation.
 */
public class ProductValidationException extends RuntimeException {

    private final transient List<Violation> violations;

    public ProductValidationException(List<Violation> violations) {
        super("Product data is invalid: " + violations);
        this.violations = List.copyOf(violations);
    }

    public List<Violation> getViolations() {
        return violations;
    }

    public record Violation(String field, String message) {
    }
}
