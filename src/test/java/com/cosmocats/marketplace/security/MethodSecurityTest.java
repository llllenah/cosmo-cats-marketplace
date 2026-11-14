package com.cosmocats.marketplace.security;

import com.cosmocats.marketplace.application.exception.CosmoCatNotFoundException;
import com.cosmocats.marketplace.application.order.OrderAccessPolicy;
import com.cosmocats.marketplace.application.order.OrderService;
import com.cosmocats.marketplace.application.order.PlaceOrderCommand;
import com.cosmocats.marketplace.application.product.ProductService;
import com.cosmocats.marketplace.application.product.ProductServiceImpl;
import com.cosmocats.marketplace.application.product.ProductValidationService;
import com.cosmocats.marketplace.application.report.ProductReportService;
import com.cosmocats.marketplace.domain.order.OrderStatus;
import com.cosmocats.marketplace.domain.product.Product;
import com.cosmocats.marketplace.infrastructure.persistence.CategoryRepository;
import com.cosmocats.marketplace.infrastructure.persistence.CosmoCatRepository;
import com.cosmocats.marketplace.infrastructure.persistence.EntityMapper;
import com.cosmocats.marketplace.infrastructure.persistence.OrderRepository;
import com.cosmocats.marketplace.infrastructure.persistence.ProductRepository;
import com.cosmocats.marketplace.infrastructure.persistence.TestEntities;
import com.cosmocats.marketplace.infrastructure.persistence.entity.CategoryEntity;
import com.cosmocats.marketplace.infrastructure.persistence.entity.CosmoCatEntity;
import com.cosmocats.marketplace.infrastructure.persistence.entity.OrderEntity;
import com.cosmocats.marketplace.infrastructure.persistence.entity.ProductEntity;
import com.cosmocats.marketplace.security.apikey.ApiKeyAuthenticationToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Role and ownership rules of the services (@PreAuthorize, @PostAuthorize). The services are real
 * and go through the security proxy, only the repositories are mocked. Mocked services, like in
 * the controller tests, would not show anything: annotations of a mock are never checked.
 */
@SpringJUnitConfig({
        MethodSecurityConfig.class,
        ProductServiceImpl.class,
        ProductReportService.class,
        OrderService.class,
        OrderAccessPolicy.class,
        EntityMapper.class
})
@DisplayName("Method level security")
class MethodSecurityTest {

    private static final UUID LUNA_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
    private static final UUID ORION_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000002");
    private static final UUID UNKNOWN_CAT_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000099");
    private static final UUID MILK_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002");
    private static final UUID FOOD_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID LUNA_CART_ID = UUID.fromString("dddddddd-0000-0000-0000-000000000001");

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductReportService productReportService;

    @Autowired
    private OrderService orderService;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private CategoryRepository categoryRepository;

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private CosmoCatRepository cosmoCatRepository;

    @MockBean
    private ProductValidationService productValidationService;

    @MockBean
    private Clock clock;

    private ProductEntity milk;

    @BeforeEach
    void catalogAndOrderOfLuna() {
        CategoryEntity food = TestEntities.category(FOOD_ID, "Cosmic food");
        milk = TestEntities.product(MILK_ID, "Galaxy cosmic milk", "4.99", 10, food);
        CosmoCatEntity luna = TestEntities.cosmoCat(LUNA_ID, "Luna", "luna@moon.space");
        CosmoCatEntity orion = TestEntities.cosmoCat(ORION_ID, "Orion", "orion@mars.space");
        OrderEntity orderOfLuna = TestEntities.order(LUNA_CART_ID, luna, OrderStatus.CREATED, Instant.parse("2026-11-14T10:00:00Z"));
        orderOfLuna.addItem(milk, 2, new BigDecimal("4.99"));

        when(productRepository.findByReference(MILK_ID)).thenReturn(Optional.of(milk));
        when(categoryRepository.findByReference(FOOD_ID)).thenReturn(Optional.of(food));
        when(cosmoCatRepository.findByReference(LUNA_ID)).thenReturn(Optional.of(luna));
        when(cosmoCatRepository.findByReference(ORION_ID)).thenReturn(Optional.of(orion));
        when(orderRepository.findByNaturalId(LUNA_CART_ID)).thenReturn(Optional.of(orderOfLuna));
    }

    @AfterEach
    void logOut() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @WithMockUser(roles = "COSMO_CAT")
    void cosmoCatReadsCatalog() {
        assertThat(productService.getProductById(MILK_ID).name()).isEqualTo("Galaxy cosmic milk");
    }

    @Test
    @WithMockUser(roles = "COSMO_CAT")
    void cosmoCatCannotChangeCatalog() {
        assertThatThrownBy(() -> productService.createProduct(milkProduct())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> productService.updateProduct(milkProduct())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> productService.deleteProduct(MILK_ID)).isInstanceOf(AccessDeniedException.class);

        // Denied before the method body: nothing was read, validated or written
        verifyNoInteractions(productRepository, productValidationService, orderRepository);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminChangesCatalog() {
        assertThat(productService.updateProduct(milkProduct()).id()).isEqualTo(MILK_ID);

        productService.deleteProduct(MILK_ID);
        verify(productRepository).delete(milk);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void authorityWithoutRolePrefixIsNotAdminRole() {
        assertThatThrownBy(() -> productService.deleteProduct(MILK_ID)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "COSMO_CAT")
    void cosmoCatCannotReadReport() {
        assertThatThrownBy(() -> productReportService.getPopularProducts(10)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(orderRepository);
    }

    @Test
    @WithMockUser(roles = "COMMITTEE")
    void committeeReadsReport() {
        assertThat(productReportService.getPopularProducts(10)).isEmpty();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminReadsReport() {
        assertThat(productReportService.getPopularProducts(10)).isEmpty();
    }

    @Test
    void cosmoCatReadsOwnOrder() {
        logInWithToken("luna@moon.space", "COSMO_CAT");

        assertThat(orderService.getOrder(LUNA_CART_ID).customerId()).isEqualTo(LUNA_ID);
    }

    @Test
    void emailOfTokenIsComparedWithoutCase() {
        logInWithToken("Luna@Moon.Space", "COSMO_CAT");

        assertThatCode(() -> orderService.getOrder(LUNA_CART_ID)).doesNotThrowAnyException();
    }

    @Test
    void cosmoCatCannotReadOrderOfAnotherCat() {
        logInWithToken("orion@mars.space", "COSMO_CAT");

        assertThatThrownBy(() -> orderService.getOrder(LUNA_CART_ID)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void tokenWithoutEmailOwnsNothing() {
        logInWithToken(null, "COSMO_CAT");

        assertThatThrownBy(() -> orderService.getOrder(LUNA_CART_ID)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminReadsAnyOrder() {
        assertThat(orderService.getOrder(LUNA_CART_ID).cartId()).isEqualTo(LUNA_CART_ID);
    }

    @Test
    void apiKeyClientIsNotCustomer() {
        SecurityContextHolder.getContext().setAuthentication(ApiKeyAuthenticationToken.authenticated(
                "intergalactic-committee", AuthorityUtils.createAuthorityList("ROLE_COMMITTEE")));

        assertThatThrownBy(() -> orderService.getOrder(LUNA_CART_ID)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> orderService.placeOrder(orderFor(LUNA_ID))).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cosmoCatCannotPlaceOrderForAnotherCat() {
        logInWithToken("luna@moon.space", "COSMO_CAT");

        assertThatThrownBy(() -> orderService.placeOrder(orderFor(ORION_ID))).isInstanceOf(AccessDeniedException.class);

        // Denied before the method body: the cart was not looked up and no stock was touched
        verifyNoInteractions(orderRepository, productRepository);
        assertThat(milk.getStockQuantity()).isEqualTo(10);
    }

    @Test
    void cosmoCatGetsSameDenialForUnknownCustomerAsForAnotherCat() {
        logInWithToken("luna@moon.space", "COSMO_CAT");

        // Not CosmoCatNotFoundException: a cat must not find out which customer ids exist
        assertThatThrownBy(() -> orderService.placeOrder(orderFor(UNKNOWN_CAT_ID))).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cosmoCatPlacesOrderForItself() {
        logInWithToken("luna@moon.space", "COSMO_CAT");
        when(clock.instant()).thenReturn(Instant.parse("2026-11-14T12:00:00Z"));
        when(orderRepository.save(any(OrderEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(orderService.placeOrder(orderFor(LUNA_ID)).customerId()).isEqualTo(LUNA_ID);
        assertThat(milk.getStockQuantity()).isEqualTo(9);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPassesCheckForAnyCustomerAndGetsBusinessError() {
        assertThatThrownBy(() -> orderService.placeOrder(orderFor(UNKNOWN_CAT_ID)))
                .isInstanceOf(CosmoCatNotFoundException.class);
    }

    private static void logInWithToken(String email, String... roles) {
        Jwt.Builder token = Jwt.withTokenValue("token").header("alg", "RS256").subject("user-1");
        if (email != null) {
            token.claim("email", email);
        }
        String[] authorities = List.of(roles).stream().map(role -> "ROLE_" + role).toArray(String[]::new);
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(token.build(), AuthorityUtils.createAuthorityList(authorities)));
    }

    private static PlaceOrderCommand orderFor(UUID customerId) {
        return new PlaceOrderCommand(UUID.fromString("dddddddd-0000-0000-0000-000000000002"), customerId,
                List.of(new PlaceOrderCommand.Line(MILK_ID, 1)));
    }

    private static Product milkProduct() {
        return new Product(MILK_ID, "Galaxy cosmic milk", "Milk from the Milky Way", new BigDecimal("4.99"), 10, FOOD_ID);
    }
}
