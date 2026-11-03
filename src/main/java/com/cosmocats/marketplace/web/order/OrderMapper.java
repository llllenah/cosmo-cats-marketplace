package com.cosmocats.marketplace.web.order;

import com.cosmocats.marketplace.application.order.PlaceOrderCommand;
import com.cosmocats.marketplace.domain.order.Order;
import com.cosmocats.marketplace.web.order.dto.OrderResponseDto;
import com.cosmocats.marketplace.web.order.dto.PlaceOrderRequestDto;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public PlaceOrderCommand toCommand(PlaceOrderRequestDto request) {
        return new PlaceOrderCommand(
                request.cartId(),
                request.customerId(),
                request.items().stream()
                        .map(item -> new PlaceOrderCommand.Line(item.productId(), item.quantity()))
                        .toList()
        );
    }

    public OrderResponseDto toDto(Order order) {
        return new OrderResponseDto(
                order.cartId(),
                order.customerId(),
                order.status(),
                order.createdAt(),
                order.items().stream()
                        .map(item -> new OrderResponseDto.Item(item.productId(), item.quantity(),
                                item.unitPrice(), item.subtotal()))
                        .toList(),
                order.totalPrice()
        );
    }
}
