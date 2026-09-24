package com.cosmocats.marketplace.application.exception;

import java.util.UUID;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(UUID categoryId) {
        super("Category with id '%s' does not exist.".formatted(categoryId));
    }
}
