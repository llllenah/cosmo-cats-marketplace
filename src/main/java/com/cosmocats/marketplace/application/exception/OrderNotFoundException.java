package com.cosmocats.marketplace.application.exception;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(UUID cartId) {
        super("Order for cart '%s' was not found.".formatted(cartId));
    }
}
