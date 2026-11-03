package com.cosmocats.marketplace.application.exception;

import java.util.UUID;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(UUID productId, int available, int requested) {
        super("Product '%s' has only %d items in stock, %d requested.".formatted(productId, available, requested));
    }
}
