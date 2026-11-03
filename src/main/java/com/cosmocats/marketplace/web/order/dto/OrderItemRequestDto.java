package com.cosmocats.marketplace.web.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record OrderItemRequestDto(

        @NotNull(message = "must not be null")
        UUID productId,

        @NotNull(message = "must not be null")
        @Positive(message = "must be greater than 0")
        @Max(value = 1000, message = "must be less than or equal to 1000")
        Integer quantity
) {
}
