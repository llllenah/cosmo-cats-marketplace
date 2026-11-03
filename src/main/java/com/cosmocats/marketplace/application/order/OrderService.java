package com.cosmocats.marketplace.application.order;

import com.cosmocats.marketplace.application.exception.CosmoCatNotFoundException;
import com.cosmocats.marketplace.application.exception.InsufficientStockException;
import com.cosmocats.marketplace.application.exception.OrderAlreadyExistsException;
import com.cosmocats.marketplace.application.exception.OrderNotFoundException;
import com.cosmocats.marketplace.application.exception.OrderedProductNotFoundException;
import com.cosmocats.marketplace.domain.cart.Cart;
import com.cosmocats.marketplace.domain.cart.CartItem;
import com.cosmocats.marketplace.domain.order.Order;
import com.cosmocats.marketplace.infrastructure.persistence.CosmoCatRepository;
import com.cosmocats.marketplace.infrastructure.persistence.EntityMapper;
import com.cosmocats.marketplace.infrastructure.persistence.OrderRepository;
import com.cosmocats.marketplace.infrastructure.persistence.ProductRepository;
import com.cosmocats.marketplace.infrastructure.persistence.entity.CosmoCatEntity;
import com.cosmocats.marketplace.infrastructure.persistence.entity.OrderEntity;
import com.cosmocats.marketplace.infrastructure.persistence.entity.ProductEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CosmoCatRepository cosmoCatRepository;
    private final EntityMapper entityMapper;
    private final Clock clock;

    /**
     * Creates the order and takes the ordered quantities from the stock in one transaction:
     * if any line fails (unknown product, not enough items), nothing is saved and the stock
     * of the products checked before stays as it was.
     */
    @Transactional
    public Order placeOrder(PlaceOrderCommand command) {
        if (orderRepository.findByNaturalId(command.cartId()).isPresent()) {
            throw new OrderAlreadyExistsException(command.cartId());
        }
        CosmoCatEntity customer = cosmoCatRepository.findByReference(command.customerId())
                .orElseThrow(() -> new CosmoCatNotFoundException(command.customerId()));

        Map<UUID, ProductEntity> products = new LinkedHashMap<>();
        List<CartItem> cartItems = new ArrayList<>();
        for (PlaceOrderCommand.Line line : command.lines()) {
            ProductEntity product = productRepository.findByReference(line.productId())
                    .orElseThrow(() -> new OrderedProductNotFoundException(line.productId()));
            takeFromStock(product, line.quantity());
            products.put(product.getReference(), product);
            cartItems.add(new CartItem(product.getReference(), line.quantity(), product.getPrice()));
        }
        Order order = Order.placeFrom(new Cart(command.cartId(), command.customerId(), cartItems), clock.instant());

        OrderEntity entity = new OrderEntity();
        entity.setCartId(order.cartId());
        entity.setCosmoCat(customer);
        entity.setStatus(order.status());
        entity.setCreatedAt(order.createdAt());
        order.items().forEach(item -> entity.addItem(products.get(item.productId()), item.quantity(), item.unitPrice()));
        return entityMapper.toDomain(orderRepository.save(entity));
    }

    public Order getOrder(UUID cartId) {
        return orderRepository.findByNaturalId(cartId)
                .map(entityMapper::toDomain)
                .orElseThrow(() -> new OrderNotFoundException(cartId));
    }

    private static void takeFromStock(ProductEntity product, int quantity) {
        if (product.getStockQuantity() < quantity) {
            throw new InsufficientStockException(product.getReference(), product.getStockQuantity(), quantity);
        }
        product.setStockQuantity(product.getStockQuantity() - quantity);
    }
}
