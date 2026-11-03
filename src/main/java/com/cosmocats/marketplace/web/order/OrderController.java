package com.cosmocats.marketplace.web.order;

import com.cosmocats.marketplace.application.order.OrderService;
import com.cosmocats.marketplace.domain.order.Order;
import com.cosmocats.marketplace.web.order.dto.OrderResponseDto;
import com.cosmocats.marketplace.web.order.dto.PlaceOrderRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * Orders are addressed by cart id (the natural id of an order), not by a generated order id.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    @PostMapping
    public ResponseEntity<OrderResponseDto> placeOrder(@Valid @RequestBody PlaceOrderRequestDto request) {
        Order order = orderService.placeOrder(orderMapper.toCommand(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{cartId}")
                .buildAndExpand(order.cartId())
                .toUri();
        return ResponseEntity.created(location).body(orderMapper.toDto(order));
    }

    @GetMapping("/{cartId}")
    public OrderResponseDto getOrder(@PathVariable UUID cartId) {
        return orderMapper.toDto(orderService.getOrder(cartId));
    }
}
