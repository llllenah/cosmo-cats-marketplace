package com.cosmocats.marketplace.web.product;

import com.cosmocats.marketplace.application.exception.ProductValidationException;
import com.cosmocats.marketplace.application.exception.ProductValidationException.Violation;
import com.cosmocats.marketplace.application.product.PageCursor;
import com.cosmocats.marketplace.application.product.PagedResult;
import com.cosmocats.marketplace.domain.product.Product;
import com.cosmocats.marketplace.web.common.PageResponse;
import com.cosmocats.marketplace.web.common.PageTokenCodec;
import com.cosmocats.marketplace.web.product.dto.ProductRequestDto;
import com.cosmocats.marketplace.web.product.dto.ProductResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

@DisplayName("ProductMapper")
class ProductMapperTest {

    private static final UUID PRODUCT_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID CATEGORY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final PageTokenCodec pageTokenCodec = new PageTokenCodec();
    private final ProductMapper mapper = new ProductMapper(pageTokenCodec);

    @Test
    void mapsCreateRequestWithoutIdAndTrimsText() {
        ProductRequestDto request = new ProductRequestDto("  Star yarn ball ", "  Soft  ",
                new BigDecimal("12.50"), 5, CATEGORY_ID);

        Product product = mapper.toDomain(request);

        assertThat(product).isEqualTo(new Product(null, "Star yarn ball", "Soft",
                new BigDecimal("12.50"), 5, CATEGORY_ID));
    }

    @Test
    void mapsUpdateRequestWithIdFromUrl() {
        ProductRequestDto request = new ProductRequestDto("Star yarn ball", null, BigDecimal.ONE, 0, CATEGORY_ID);

        Product product = mapper.toDomain(PRODUCT_ID, request);

        assertThat(product.id()).isEqualTo(PRODUCT_ID);
        assertThat(product.description()).isNull();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequests")
    void rejectsRequestThatBreaksBasicRules(String description, ProductRequestDto request, Violation expected) {
        assertThatThrownBy(() -> mapper.toDomain(request))
                .isInstanceOf(ProductValidationException.class)
                .extracting(ex -> ((ProductValidationException) ex).getViolations(), list(Violation.class))
                .containsExactly(expected);
    }

    @Test
    void collectsAllViolationsAtOnce() {
        ProductRequestDto request = new ProductRequestDto(null, null, null, null, null);

        assertThatThrownBy(() -> mapper.toDomain(request))
                .isInstanceOf(ProductValidationException.class)
                .hasMessageContaining("Product data is invalid")
                .extracting(ex -> ((ProductValidationException) ex).getViolations(), list(Violation.class))
                .extracting(Violation::field)
                .containsExactly("name", "price", "stockQuantity", "categoryId");
    }

    @Test
    void mapsProductToResponse() {
        Product product = new Product(PRODUCT_ID, "Star yarn ball", "Soft", new BigDecimal("12.50"), 5, CATEGORY_ID);

        assertThat(mapper.toDto(product)).isEqualTo(new ProductResponseDto(PRODUCT_ID, "Star yarn ball", "Soft",
                new BigDecimal("12.50"), 5, CATEGORY_ID));
    }

    @Test
    void mapsPageWithEncodedNextPageToken() {
        Product product = new Product(PRODUCT_ID, "Star yarn ball", null, BigDecimal.TEN, 5, CATEGORY_ID);
        PageCursor nextCursor = PageCursor.after(product);

        PageResponse<ProductResponseDto> page = mapper.toPageResponse(
                new PagedResult<>(List.of(product), 1, 3, nextCursor));

        assertThat(page.content()).extracting(ProductResponseDto::id).containsExactly(PRODUCT_ID);
        assertThat(page.page().size()).isEqualTo(1);
        assertThat(page.page().totalElements()).isEqualTo(3);
        assertThat(pageTokenCodec.decode(page.page().nextPageToken())).isEqualTo(nextCursor);
    }

    @Test
    void mapsLastPageWithoutNextPageToken() {
        PageResponse<ProductResponseDto> page = mapper.toPageResponse(new PagedResult<>(List.of(), 20, 0, null));

        assertThat(page.content()).isEmpty();
        assertThat(page.page().nextPageToken()).isNull();
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of("blank name", request("   ", BigDecimal.ONE, 1, CATEGORY_ID),
                        new Violation("name", "must not be blank")),
                Arguments.of("missing price", request("Star ball", null, 1, CATEGORY_ID),
                        new Violation("price", "must not be null")),
                Arguments.of("zero price", request("Star ball", BigDecimal.ZERO, 1, CATEGORY_ID),
                        new Violation("price", "must be greater than 0")),
                Arguments.of("price with 3 decimals", request("Star ball", new BigDecimal("1.005"), 1, CATEGORY_ID),
                        new Violation("price", "must have at most 2 decimal places")),
                Arguments.of("missing stock", request("Star ball", BigDecimal.ONE, null, CATEGORY_ID),
                        new Violation("stockQuantity", "must not be null")),
                Arguments.of("negative stock", request("Star ball", BigDecimal.ONE, -1, CATEGORY_ID),
                        new Violation("stockQuantity", "must be greater than or equal to 0")),
                Arguments.of("missing category", request("Star ball", BigDecimal.ONE, 1, null),
                        new Violation("categoryId", "must not be null"))
        );
    }

    private static ProductRequestDto request(String name, BigDecimal price, Integer stock, UUID categoryId) {
        return new ProductRequestDto(name, null, price, stock, categoryId);
    }
}
