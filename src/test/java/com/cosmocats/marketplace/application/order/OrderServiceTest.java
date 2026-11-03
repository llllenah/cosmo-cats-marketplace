package com.cosmocats.marketplace.application.order;

import com.cosmocats.marketplace.application.exception.CosmoCatNotFoundException;
import com.cosmocats.marketplace.application.exception.InsufficientStockException;
import com.cosmocats.marketplace.application.exception.OrderAlreadyExistsException;
import com.cosmocats.marketplace.application.exception.OrderNotFoundException;
import com.cosmocats.marketplace.application.exception.OrderedProductNotFoundException;
import com.cosmocats.marketplace.domain.order.Order;
import com.cosmocats.marketplace.domain.order.OrderItem;
import com.cosmocats.marketplace.domain.order.OrderStatus;
import com.cosmocats.marketplace.infrastructure.persistence.CosmoCatRepository;
import com.cosmocats.marketplace.infrastructure.persistence.EntityMapper;
import com.cosmocats.marketplace.infrastructure.persistence.OrderRepository;
import com.cosmocats.marketplace.infrastructure.persistence.ProductRepository;
import com.cosmocats.marketplace.infrastructure.persistence.entity.CategoryEntity;
import com.cosmocats.marketplace.infrastructure.persistence.entity.CosmoCatEntity;
import com.cosmocats.marketplace.infrastructure.persistence.entity.OrderEntity;
import com.cosmocats.marketplace.infrastructure.persistence.entity.ProductEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.cosmocats.marketplace.infrastructure.persistence.TestEntities.category;
import static com.cosmocats.marketplace.infrastructure.persistence.TestEntities.cosmoCat;
import static com.cosmocats.marketplace.infrastructure.persistence.TestEntities.order;
import static com.cosmocats.marketplace.infrastructure.persistence.TestEntities.product;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService")
class OrderServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-03T12:00:00Z");
    private static final UUID CART_ID = UUID.fromString("dddddddd-0000-0000-0000-000000000001");
    private static final UUID LUNA_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
    private static final UUID MILK_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002");
    private static final UUID CAPSULE_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000004");

    private final CategoryEntity food = category(UUID.randomUUID(), "Cosmic food");
    private final CosmoCatEntity luna = cosmoCat(LUNA_ID, "Luna", "luna@moon.space");

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CosmoCatRepository cosmoCatRepository;

    @Captor
    private ArgumentCaptor<OrderEntity> orderCaptor;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, productRepository, cosmoCatRepository,
                new EntityMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Nested
    @DisplayName("placeOrder")
    class PlaceOrder {

        @Test
        void savesOrderWithCatalogPricesAndTakesItemsFromStock() {
            ProductEntity milk = product(MILK_ID, "Galaxy cosmic milk", "4.99", 300, food);
            ProductEntity capsule = product(CAPSULE_ID, "Nebula nap capsule", "89.90", 10, food);
            when(orderRepository.findByNaturalId(CART_ID)).thenReturn(Optional.empty());
            when(cosmoCatRepository.findByReference(LUNA_ID)).thenReturn(Optional.of(luna));
            when(productRepository.findByReference(MILK_ID)).thenReturn(Optional.of(milk));
            when(productRepository.findByReference(CAPSULE_ID)).thenReturn(Optional.of(capsule));
            when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            Order order = orderService.placeOrder(command(new PlaceOrderCommand.Line(MILK_ID, 2),
                    new PlaceOrderCommand.Line(CAPSULE_ID, 10)));

            assertThat(order).isEqualTo(new Order(CART_ID, LUNA_ID, List.of(
                    new OrderItem(MILK_ID, 2, new BigDecimal("4.99")),
                    new OrderItem(CAPSULE_ID, 10, new BigDecimal("89.90"))), OrderStatus.CREATED, NOW));
            assertThat(order.totalPrice()).isEqualByComparingTo("908.98");
            assertThat(milk.getStockQuantity()).isEqualTo(298);
            assertThat(capsule.getStockQuantity()).isZero();

            verify(orderRepository).save(orderCaptor.capture());
            OrderEntity saved = orderCaptor.getValue();
            assertThat(saved.getCosmoCat()).isSameAs(luna);
            assertThat(saved.getItems()).allSatisfy(item -> assertThat(item.getOrder()).isSameAs(saved));
            assertThat(saved.getItems()).extracting(item -> item.getProduct().getReference())
                    .containsExactly(MILK_ID, CAPSULE_ID);
        }

        @Test
        void rejectsCartThatIsAlreadyCheckedOut() {
            when(orderRepository.findByNaturalId(CART_ID))
                    .thenReturn(Optional.of(order(CART_ID, luna, OrderStatus.CREATED, NOW)));

            assertThatThrownBy(() -> orderService.placeOrder(command(new PlaceOrderCommand.Line(MILK_ID, 1))))
                    .isInstanceOf(OrderAlreadyExistsException.class)
                    .hasMessage("Cart '%s' is already checked out.", CART_ID);
            verifyNoInteractions(productRepository);
            verify(orderRepository, never()).save(any());
        }

        @Test
        void rejectsUnknownCosmoCat() {
            when(orderRepository.findByNaturalId(CART_ID)).thenReturn(Optional.empty());
            when(cosmoCatRepository.findByReference(LUNA_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.placeOrder(command(new PlaceOrderCommand.Line(MILK_ID, 1))))
                    .isInstanceOf(CosmoCatNotFoundException.class)
                    .hasMessage("Cosmo cat with id '%s' does not exist.", LUNA_ID);
            verify(orderRepository, never()).save(any());
        }

        @Test
        void rejectsUnknownProduct() {
            when(orderRepository.findByNaturalId(CART_ID)).thenReturn(Optional.empty());
            when(cosmoCatRepository.findByReference(LUNA_ID)).thenReturn(Optional.of(luna));
            when(productRepository.findByReference(MILK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.placeOrder(command(new PlaceOrderCommand.Line(MILK_ID, 1))))
                    .isInstanceOf(OrderedProductNotFoundException.class)
                    .hasMessage("Product with id '%s' does not exist.", MILK_ID);
            verify(orderRepository, never()).save(any());
        }

        @Test
        void rejectsQuantityAboveStockAndKeepsThatStock() {
            ProductEntity capsule = product(CAPSULE_ID, "Nebula nap capsule", "89.90", 10, food);
            when(orderRepository.findByNaturalId(CART_ID)).thenReturn(Optional.empty());
            when(cosmoCatRepository.findByReference(LUNA_ID)).thenReturn(Optional.of(luna));
            when(productRepository.findByReference(CAPSULE_ID)).thenReturn(Optional.of(capsule));

            assertThatThrownBy(() -> orderService.placeOrder(command(new PlaceOrderCommand.Line(CAPSULE_ID, 11))))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessage("Product '%s' has only 10 items in stock, 11 requested.", CAPSULE_ID);
            assertThat(capsule.getStockQuantity()).isEqualTo(10);
            verify(orderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getOrder")
    class GetOrder {

        @Test
        void findsOrderByCartId() {
            OrderEntity entity = order(CART_ID, luna, OrderStatus.PAID, NOW);
            entity.addItem(product(MILK_ID, "Galaxy cosmic milk", "4.99", 300, food), 3, new BigDecimal("4.50"));
            when(orderRepository.findByNaturalId(CART_ID)).thenReturn(Optional.of(entity));

            assertThat(orderService.getOrder(CART_ID)).isEqualTo(new Order(CART_ID, LUNA_ID,
                    List.of(new OrderItem(MILK_ID, 3, new BigDecimal("4.50"))), OrderStatus.PAID, NOW));
        }

        @Test
        void throwsNotFoundForUnknownCart() {
            when(orderRepository.findByNaturalId(CART_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrder(CART_ID))
                    .isInstanceOf(OrderNotFoundException.class)
                    .hasMessage("Order for cart '%s' was not found.", CART_ID);
        }
    }

    private static PlaceOrderCommand command(PlaceOrderCommand.Line... lines) {
        return new PlaceOrderCommand(CART_ID, LUNA_ID, List.of(lines));
    }
}
