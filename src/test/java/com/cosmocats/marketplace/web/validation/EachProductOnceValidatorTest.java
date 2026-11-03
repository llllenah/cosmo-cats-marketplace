package com.cosmocats.marketplace.web.validation;

import com.cosmocats.marketplace.web.order.dto.OrderItemRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EachProductOnceValidator")
class EachProductOnceValidatorTest {

    private static final UUID MILK_ID = UUID.randomUUID();
    private static final UUID CAPSULE_ID = UUID.randomUUID();

    private final EachProductOnceValidator validator = new EachProductOnceValidator();

    @Test
    void acceptsDifferentProducts() {
        assertThat(validator.isValid(List.of(item(MILK_ID, 1), item(CAPSULE_ID, 2)), null)).isTrue();
    }

    @Test
    void rejectsSameProductTwiceEvenWithDifferentQuantities() {
        assertThat(validator.isValid(List.of(item(MILK_ID, 1), item(CAPSULE_ID, 1), item(MILK_ID, 3)), null)).isFalse();
    }

    @Test
    void leavesNullsToOtherConstraints() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid(Arrays.asList(null, item(null, 1), item(null, 2), item(MILK_ID, 1)), null)).isTrue();
    }

    private static OrderItemRequestDto item(UUID productId, int quantity) {
        return new OrderItemRequestDto(productId, quantity);
    }
}
