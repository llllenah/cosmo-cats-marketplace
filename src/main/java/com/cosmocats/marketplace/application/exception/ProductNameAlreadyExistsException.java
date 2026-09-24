package com.cosmocats.marketplace.application.exception;

public class ProductNameAlreadyExistsException extends RuntimeException {

    public ProductNameAlreadyExistsException(String productName) {
        super("Product with name '%s' already exists.".formatted(productName));
    }
}
