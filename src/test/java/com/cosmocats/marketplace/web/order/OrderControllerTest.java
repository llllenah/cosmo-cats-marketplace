package com.cosmocats.marketplace.web.order;

import com.cosmocats.marketplace.application.exception.CosmoCatNotFoundException;
import com.cosmocats.marketplace.application.exception.InsufficientStockException;
import com.cosmocats.marketplace.application.exception.OrderAlreadyExistsException;
import com.cosmocats.marketplace.application.exception.OrderNotFoundException;
import com.cosmocats.marketplace.application.exception.OrderedProductNotFoundException;
import com.cosmocats.marketplace.application.order.OrderService;
import com.cosmocats.marketplace.application.order.PlaceOrderCommand;
import com.cosmocats.marketplace.domain.order.Order;
import com.cosmocats.marketplace.domain.order.OrderItem;
import com.cosmocats.marketplace.domain.order.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(OrderMapper.class)
@DisplayName("OrderController")
class OrderControllerTest {

    private static final String ORDERS_URL = "/api/v1/orders";
    private static final String ERRORS_BASE_URL = "https://cosmo-cats.market/errors/";
    private static final UUID CART_ID = UUID.fromString("dddddddd-0000-0000-0000-000000000001");
    private static final UUID LUNA_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
    private static final UUID MILK_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002");
    private static final UUID CAPSULE_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000004");
    private static final Instant CREATED_AT = Instant.parse("2026-11-03T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Nested
    @DisplayName("POST /api/v1/orders")
    class PlaceOrder {

        @Test
        void returns201WithLocationAndOrderWithTotals() throws Exception {
            when(orderService.placeOrder(any())).thenReturn(order());

            postOrder(validRequest())
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", endsWith("/api/v1/orders/" + CART_ID)))
                    .andExpect(jsonPath("$.cartId").value(CART_ID.toString()))
                    .andExpect(jsonPath("$.customerId").value(LUNA_ID.toString()))
                    .andExpect(jsonPath("$.status").value("CREATED"))
                    .andExpect(jsonPath("$.createdAt").value("2026-11-03T12:00:00Z"))
                    .andExpect(jsonPath("$.items[0].productId").value(MILK_ID.toString()))
                    .andExpect(jsonPath("$.items[0].quantity").value(2))
                    .andExpect(jsonPath("$.items[0].unitPrice").value(4.99))
                    .andExpect(jsonPath("$.items[0].subtotal").value(9.98))
                    .andExpect(jsonPath("$.items[1].subtotal").value(89.90))
                    .andExpect(jsonPath("$.totalPrice").value(99.88));

            verify(orderService).placeOrder(new PlaceOrderCommand(CART_ID, LUNA_ID, List.of(
                    new PlaceOrderCommand.Line(MILK_ID, 2), new PlaceOrderCommand.Line(CAPSULE_ID, 1))));
        }

        static Stream<Arguments> invalidRequests() {
            return Stream.of(
                    Arguments.of("""
                            {"customerId": "%s", "items": [{"productId": "%s", "quantity": 1}]}
                            """.formatted(LUNA_ID, MILK_ID), "cartId", "must not be null"),
                    Arguments.of("""
                            {"cartId": "%s", "items": [{"productId": "%s", "quantity": 1}]}
                            """.formatted(CART_ID, MILK_ID), "customerId", "must not be null"),
                    Arguments.of("""
                            {"cartId": "%s", "customerId": "%s", "items": []}
                            """.formatted(CART_ID, LUNA_ID), "items", "must not be empty"),
                    Arguments.of("""
                            {"cartId": "%s", "customerId": "%s", "items": [{"productId": "%s", "quantity": 0}]}
                            """.formatted(CART_ID, LUNA_ID, MILK_ID), "items[0].quantity", "must be greater than 0"),
                    Arguments.of("""
                            {"cartId": "%s", "customerId": "%s", "items": [{"productId": "%s", "quantity": 1001}]}
                            """.formatted(CART_ID, LUNA_ID, MILK_ID), "items[0].quantity", "must be less than or equal to 1000"),
                    Arguments.of("""
                            {"cartId": "%s", "customerId": "%s", "items": [{"quantity": 1}]}
                            """.formatted(CART_ID, LUNA_ID), "items[0].productId", "must not be null"),
                    Arguments.of("""
                            {"cartId": "%s", "customerId": "%s", "items": [null]}
                            """.formatted(CART_ID, LUNA_ID), "items[0]", "must not be null"),
                    Arguments.of("""
                            {"cartId": "%s", "customerId": "%s", "items": [
                              {"productId": "%s", "quantity": 1}, {"productId": "%s", "quantity": 2}]}
                            """.formatted(CART_ID, LUNA_ID, MILK_ID, MILK_ID), "items", "must not contain the same product twice"));
        }

        @ParameterizedTest(name = "{1} {2}")
        @MethodSource("invalidRequests")
        void returns400ForInvalidRequest(String body, String field, String message) throws Exception {
            postOrder(body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value(ERRORS_BASE_URL + "validation"))
                    .andExpect(jsonPath("$.errors[0].field").value(field))
                    .andExpect(jsonPath("$.errors[0].message").value(message));
            verifyNoInteractions(orderService);
        }

        static Stream<Arguments> serviceErrors() {
            return Stream.of(
                    Arguments.of(new OrderAlreadyExistsException(CART_ID), 409, "order-already-exists"),
                    Arguments.of(new InsufficientStockException(MILK_ID, 1, 2), 409, "insufficient-stock"),
                    Arguments.of(new ObjectOptimisticLockingFailureException("ProductEntity", 1L), 409, "concurrent-update"),
                    Arguments.of(new CannotAcquireLockException("deadlock detected"), 409, "concurrent-update"),
                    Arguments.of(new DataIntegrityViolationException("uq_orders_cart_id"), 409, "data-conflict"),
                    Arguments.of(new CosmoCatNotFoundException(LUNA_ID), 422, "order-reference-not-found"),
                    Arguments.of(new OrderedProductNotFoundException(MILK_ID), 422, "order-reference-not-found"));
        }

        @ParameterizedTest(name = "{2}")
        @MethodSource("serviceErrors")
        void convertsServiceErrorsToProblemDetails(RuntimeException error, int status, String type) throws Exception {
            when(orderService.placeOrder(any())).thenThrow(error);

            postOrder(validRequest())
                    .andExpect(status().is(status))
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.type").value(ERRORS_BASE_URL + type))
                    .andExpect(jsonPath("$.instance").value(ORDERS_URL));
        }

        @Test
        void doesNotExposeConstraintNamesOrSqlInDataConflict() throws Exception {
            when(orderService.placeOrder(any())).thenThrow(new DataIntegrityViolationException(
                    "could not execute statement [ERROR: duplicate key value violates unique constraint \"uq_orders_cart_id\"]"));

            postOrder(validRequest())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("The request conflicts with existing data."));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/orders/{cartId}")
    class GetOrder {

        @Test
        void returnsOrderByCartId() throws Exception {
            when(orderService.getOrder(CART_ID)).thenReturn(order());

            mockMvc.perform(get(ORDERS_URL + "/{cartId}", CART_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cartId").value(CART_ID.toString()))
                    .andExpect(jsonPath("$.items.length()").value(2));
        }

        @Test
        void returns404ForUnknownCart() throws Exception {
            when(orderService.getOrder(CART_ID)).thenThrow(new OrderNotFoundException(CART_ID));

            mockMvc.perform(get(ORDERS_URL + "/{cartId}", CART_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value(ERRORS_BASE_URL + "order-not-found"))
                    .andExpect(jsonPath("$.detail").value("Order for cart '" + CART_ID + "' was not found."));
        }

        @Test
        void returns400WhenCartIdIsNotUuid() throws Exception {
            mockMvc.perform(get(ORDERS_URL + "/not-a-uuid"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(orderService);
        }
    }

    private ResultActions postOrder(String body) throws Exception {
        return mockMvc.perform(post(ORDERS_URL).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String validRequest() {
        return """
                {"cartId": "%s", "customerId": "%s", "items": [
                  {"productId": "%s", "quantity": 2},
                  {"productId": "%s", "quantity": 1}]}
                """.formatted(CART_ID, LUNA_ID, MILK_ID, CAPSULE_ID);
    }

    private static Order order() {
        return new Order(CART_ID, LUNA_ID, List.of(
                new OrderItem(MILK_ID, 2, new BigDecimal("4.99")),
                new OrderItem(CAPSULE_ID, 1, new BigDecimal("89.90"))), OrderStatus.CREATED, CREATED_AT);
    }
}
