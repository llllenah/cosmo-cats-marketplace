package com.cosmocats.marketplace.application.exception;

import java.util.UUID;

/**
 * A product in the order request does not exist. Unlike ProductNotFoundException this is
 * a problem with the request body, not with the requested URL.
 */
public class OrderedProductNotFoundException extends RuntimeException {

    public OrderedProductNotFoundException(UUID productId) {
        super("Product with id '%s' does not exist.".formatted(productId));
    }
}
