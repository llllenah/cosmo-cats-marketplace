package com.cosmocats.marketplace.application.exception;

import java.util.UUID;

public class OrderAlreadyExistsException extends RuntimeException {

    public OrderAlreadyExistsException(UUID cartId) {
        super("Cart '%s' is already checked out.".formatted(cartId));
    }
}
