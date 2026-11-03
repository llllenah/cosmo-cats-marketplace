package com.cosmocats.marketplace.web.order.dto;

import com.cosmocats.marketplace.web.validation.EachProductOnce;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Checkout of a cart. Prices are not accepted from the client, they come from the catalog.
 */
public record PlaceOrderRequestDto(

        @NotNull(message = "must not be null")
        UUID cartId,

        @NotNull(message = "must not be null")
        UUID customerId,

        @NotEmpty(message = "must not be empty")
        @Size(max = 50, message = "must contain at most 50 items")
        @EachProductOnce
        List<@Valid @NotNull(message = "must not be null") OrderItemRequestDto> items
) {
}
