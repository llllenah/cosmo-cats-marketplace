package com.cosmocats.marketplace.application.order;

import java.util.List;
import java.util.UUID;

/**
 * Checkout of a cart: which products and how many. Prices are taken from the catalog, not from the client.
 */
public record PlaceOrderCommand(UUID cartId, UUID customerId, List<Line> lines) {

    public PlaceOrderCommand {
        lines = List.copyOf(lines);
    }

    public record Line(UUID productId, int quantity) {
    }
}
