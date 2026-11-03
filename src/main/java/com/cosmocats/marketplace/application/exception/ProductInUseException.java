package com.cosmocats.marketplace.application.exception;

import java.util.UUID;

public class ProductInUseException extends RuntimeException {

    public ProductInUseException(UUID productId) {
        super("Product '%s' is part of existing orders and cannot be deleted.".formatted(productId));
    }
}
