package com.cosmocats.marketplace.web.product.dto;

import com.cosmocats.marketplace.web.validation.CosmicWordCheck;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request body for creating and fully updating a product.
 * The id is not accepted from the client: it comes from the URL or is generated.
 */
public record ProductRequestDto(

        @NotBlank(message = "must not be blank")
        @Size(min = 3, max = 100, message = "must be between 3 and 100 characters")
        @CosmicWordCheck
        String name,

        @Size(max = 500, message = "must be at most 500 characters")
        String description,

        @NotNull(message = "must not be null")
        @Positive(message = "must be greater than 0")
        @Digits(integer = 10, fraction = 2, message = "must have at most 10 integer digits and 2 decimal places")
        BigDecimal price,

        @NotNull(message = "must not be null")
        @PositiveOrZero(message = "must be greater than or equal to 0")
        Integer stockQuantity,

        @NotNull(message = "must not be null")
        UUID categoryId
) {
}
