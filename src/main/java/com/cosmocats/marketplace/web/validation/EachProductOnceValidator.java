package com.cosmocats.marketplace.web.validation;

import com.cosmocats.marketplace.web.order.dto.OrderItemRequestDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class EachProductOnceValidator implements ConstraintValidator<EachProductOnce, List<OrderItemRequestDto>> {

    @Override
    public boolean isValid(List<OrderItemRequestDto> items, ConstraintValidatorContext context) {
        if (items == null) {
            return true;
        }
        Set<UUID> productIds = new HashSet<>();
        return items.stream()
                .filter(Objects::nonNull)
                .map(OrderItemRequestDto::productId)
                .filter(Objects::nonNull)
                .allMatch(productIds::add);
    }
}
