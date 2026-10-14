package com.cosmocats.marketplace.web.product;

import com.cosmocats.marketplace.application.exception.CategoryNotFoundException;
import com.cosmocats.marketplace.application.exception.ExchangeRateUnavailableException;
import com.cosmocats.marketplace.application.exception.ProductNameAlreadyExistsException;
import com.cosmocats.marketplace.application.exception.ProductNotFoundException;
import com.cosmocats.marketplace.application.exception.UnsupportedCurrencyException;
import com.cosmocats.marketplace.application.pricing.ProductPrice;
import com.cosmocats.marketplace.application.pricing.ProductPriceService;
import com.cosmocats.marketplace.application.product.PageCursor;
import com.cosmocats.marketplace.application.product.PagedResult;
import com.cosmocats.marketplace.application.product.ProductService;
import com.cosmocats.marketplace.domain.product.Product;
import com.cosmocats.marketplace.web.common.PageTokenCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import({ProductMapper.class, PageTokenCodec.class})
@DisplayName("ProductController")
class ProductControllerTest {

    private static final String PRODUCTS_URL = "/api/v1/products";
    private static final String PRODUCT_URL = "/api/v1/products/{id}";
    private static final UUID PRODUCT_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID CATEGORY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String ERRORS_BASE_URL = "https://cosmo-cats.market/errors/";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PageTokenCodec pageTokenCodec;

    @MockBean
    private ProductService productService;

    @MockBean
    private ProductPriceService productPriceService;

    @Nested
    @DisplayName("POST /api/v1/products")
    class CreateProduct {

        @Test
        void returns201WithLocationAndCreatedProduct() throws Exception {
            when(productService.createProduct(any())).thenAnswer(invocation ->
                    ((Product) invocation.getArgument(0)).withId(PRODUCT_ID));

            postProduct(validRequest())
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", endsWith("/api/v1/products/" + PRODUCT_ID)))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(PRODUCT_ID.toString()))
                    .andExpect(jsonPath("$.name").value("Moon dust catnip"))
                    .andExpect(jsonPath("$.description").value("Collected on the dark side"))
                    .andExpect(jsonPath("$.price").value(7.25))
                    .andExpect(jsonPath("$.stockQuantity").value(50))
                    .andExpect(jsonPath("$.categoryId").value(CATEGORY_ID.toString()));
        }

        @Test
        void passesTrimmedValuesToServiceAndNeverTakesIdFromBody() throws Exception {
            when(productService.createProduct(any())).thenAnswer(invocation ->
                    ((Product) invocation.getArgument(0)).withId(PRODUCT_ID));
            Map<String, Object> request = validRequest();
            request.put("id", UUID.randomUUID().toString());
            request.put("name", "  Moon dust catnip  ");
            request.put("description", "  Collected on the dark side  ");

            postProduct(request).andExpect(status().isCreated());

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productService).createProduct(captor.capture());
            assertThat(captor.getValue()).isEqualTo(new Product(null, "Moon dust catnip",
                    "Collected on the dark side", new BigDecimal("7.25"), 50, CATEGORY_ID));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.cosmocats.marketplace.web.product.ProductControllerTest#validRequestVariants")
        void acceptsValidBoundaryValues(String description, String field, Object value) throws Exception {
            when(productService.createProduct(any())).thenAnswer(invocation ->
                    ((Product) invocation.getArgument(0)).withId(PRODUCT_ID));

            postProduct(validRequestWith(field, value)).andExpect(status().isCreated());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.cosmocats.marketplace.web.product.ProductControllerTest#invalidFieldValues")
        void rejectsInvalidFieldWith400AndDoesNotCallService(String description, String field, Object value,
                                                             String expectedMessage) throws Exception {
            postProduct(validRequestWith(field, value))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.type").value(ERRORS_BASE_URL + "validation"))
                    .andExpect(jsonPath("$.title").value("Validation failed"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.instance").value(PRODUCTS_URL))
                    .andExpect(jsonPath("$.detail").value("Field '%s' %s.".formatted(field, expectedMessage)))
                    .andExpect(jsonPath("$.errors.length()").value(1))
                    .andExpect(jsonPath("$.errors[0].field").value(field))
                    .andExpect(jsonPath("$.errors[0].message").value(expectedMessage));

            verifyNoInteractions(productService);
        }

        @Test
        void reportsAllInvalidFieldsSortedByFieldAndMessage() throws Exception {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("name", "");
            request.put("price", 0);
            request.put("stockQuantity", -1);

            postProduct(request)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[*].field").value(contains(
                            "categoryId", "name", "name", "name", "price", "stockQuantity")))
                    .andExpect(jsonPath("$.errors[*].message").value(contains(
                            "must not be null",
                            "must be between 3 and 100 characters",
                            "must contain at least one cosmic word (star, galaxy, comet, cosmic, space, planet, moon, nebula, orbit, asteroid)",
                            "must not be blank",
                            "must be greater than 0",
                            "must be greater than or equal to 0")));

            verifyNoInteractions(productService);
        }

        @Test
        void rejectsMalformedJson() throws Exception {
            mockMvc.perform(post(PRODUCTS_URL).contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Star"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.type").value(ERRORS_BASE_URL + "malformed-request"))
                    .andExpect(jsonPath("$.detail").value("Request body is missing or is not valid JSON."));
        }

        @Test
        void rejectsJsonArrayInsteadOfObject() throws Exception {
            mockMvc.perform(post(PRODUCTS_URL).contentType(MediaType.APPLICATION_JSON).content("[]"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value(ERRORS_BASE_URL + "malformed-request"));

            verifyNoInteractions(productService);
        }

        @Test
        void rejectsMissingBody() throws Exception {
            mockMvc.perform(post(PRODUCTS_URL).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value(ERRORS_BASE_URL + "malformed-request"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.cosmocats.marketplace.web.product.ProductControllerTest#valuesOfWrongType")
        void rejectsValueOfWrongTypeAndNamesTheField(String description, String field, Object value) throws Exception {
            postProduct(validRequestWith(field, value))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value(ERRORS_BASE_URL + "validation"))
                    .andExpect(jsonPath("$.errors.length()").value(1))
                    .andExpect(jsonPath("$.errors[0].field").value(field))
                    .andExpect(jsonPath("$.errors[0].message").value("has invalid value"));

            verifyNoInteractions(productService);
        }

        @Test
        void rejectsUnsupportedContentType() throws Exception {
            mockMvc.perform(post(PRODUCTS_URL).contentType(MediaType.TEXT_PLAIN).content("Star yarn ball"))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        }

        @Test
        void returns409WhenNameIsTaken() throws Exception {
            when(productService.createProduct(any()))
                    .thenThrow(new ProductNameAlreadyExistsException("Moon dust catnip"));

            postProduct(validRequest())
                    .andExpect(status().isConflict())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.type").value(ERRORS_BASE_URL + "product-already-exists"))
                    .andExpect(jsonPath("$.title").value("Product already exists"))
                    .andExpect(jsonPath("$.detail").value("Product with name 'Moon dust catnip' already exists."))
                    .andExpect(jsonPath("$.instance").value(PRODUCTS_URL));
        }

        @Test
        void returns422WhenCategoryDoesNotExist() throws Exception {
            when(productService.createProduct(any())).thenThrow(new CategoryNotFoundException(CATEGORY_ID));

            postProduct(validRequest())
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.type").value(ERRORS_BASE_URL + "category-not-found"))
                    .andExpect(jsonPath("$.detail").value("Category with id '%s' does not exist.".formatted(CATEGORY_ID)));
        }

        @Test
        void returns500WithoutInternalDetailsOnUnexpectedError() throws Exception {
            when(productService.createProduct(any()))
                    .thenThrow(new IllegalStateException("connection to secret-db:5432 refused"));

            postProduct(validRequest())
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.type").value(ERRORS_BASE_URL + "internal-error"))
                    .andExpect(jsonPath("$.detail").value("Something went wrong on our side. Please try again later."))
                    .andExpect(content().string(not(containsString("secret-db"))));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products")
    class GetProducts {

        @Test
        void usesDefaultPageSizeAndNoCursorForFirstPage() throws Exception {
            when(productService.getProducts(null, 20)).thenReturn(new PagedResult<>(List.of(), 20, 0, null));

            mockMvc.perform(get(PRODUCTS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.page.size").value(20))
                    .andExpect(jsonPath("$.page.totalElements").value(0))
                    .andExpect(jsonPath("$.page.nextPageToken").value(nullValue()));
        }

        @Test
        void returnsPageWithNextPageToken() throws Exception {
            Product product = product(PRODUCT_ID, "Anti-gravity star yarn ball");
            PageCursor nextCursor = PageCursor.after(product);
            when(productService.getProducts(null, 1)).thenReturn(new PagedResult<>(List.of(product), 1, 4, nextCursor));

            mockMvc.perform(get(PRODUCTS_URL).param("pageSize", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(PRODUCT_ID.toString()))
                    .andExpect(jsonPath("$.page.size").value(1))
                    .andExpect(jsonPath("$.page.totalElements").value(4))
                    .andExpect(jsonPath("$.page.nextPageToken").value(pageTokenCodec.encode(nextCursor)));
        }

        @Test
        void decodesPageTokenIntoCursor() throws Exception {
            PageCursor cursor = new PageCursor("Comet tail scratching post", PRODUCT_ID);
            when(productService.getProducts(cursor, 100)).thenReturn(new PagedResult<>(List.of(), 100, 4, null));

            mockMvc.perform(get(PRODUCTS_URL)
                            .param("pageSize", "100")
                            .param("pageToken", pageTokenCodec.encode(cursor)))
                    .andExpect(status().isOk());

            verify(productService).getProducts(cursor, 100);
        }

        @ParameterizedTest(name = "pageSize={0}")
        @ValueSource(strings = {"0", "-1"})
        void rejectsPageSizeBelowOne(String pageSize) throws Exception {
            expectPageSizeError(pageSize, "must be greater than or equal to 1");
        }

        @Test
        void rejectsPageSizeAboveMaximum() throws Exception {
            expectPageSizeError("101", "must be less than or equal to 100");
        }

        @ParameterizedTest(name = "pageSize={0}")
        @ValueSource(strings = {"abc", "1.5", "99999999999"})
        void rejectsPageSizeThatIsNotInteger(String pageSize) throws Exception {
            expectPageSizeError(pageSize, "has invalid value");
        }

        @ParameterizedTest(name = "pageToken={0}")
        @ValueSource(strings = {"not base64 !", "bm8tc2VwYXJhdG9y", "bm90LWEtdXVpZDpTdGFy"})
        void rejectsPageTokenThatCannotBeDecoded(String pageToken) throws Exception {
            mockMvc.perform(get(PRODUCTS_URL).param("pageToken", pageToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.type").value(ERRORS_BASE_URL + "invalid-page-token"))
                    .andExpect(jsonPath("$.detail").value("Page token is invalid. Request the first page without pageToken."));

            verifyNoInteractions(productService);
        }

        @Test
        void rejectsTooLongPageToken() throws Exception {
            mockMvc.perform(get(PRODUCTS_URL).param("pageToken", "a".repeat(513)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("pageToken"))
                    .andExpect(jsonPath("$.errors[0].message").value("must be at most 512 characters"));
        }

        private void expectPageSizeError(String pageSize, String message) throws Exception {
            mockMvc.perform(get(PRODUCTS_URL).param("pageSize", pageSize))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value(ERRORS_BASE_URL + "validation"))
                    .andExpect(jsonPath("$.detail").value("Field 'pageSize' %s.".formatted(message)))
                    .andExpect(jsonPath("$.errors[0].field").value("pageSize"))
                    .andExpect(jsonPath("$.errors[0].message").value(message));

            verify(productService, never()).getProducts(any(), anyInt());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products/{id}")
    class GetProductById {

        @Test
        void returnsProduct() throws Exception {
            when(productService.getProductById(PRODUCT_ID)).thenReturn(product(PRODUCT_ID, "Galaxy cosmic milk"));

            mockMvc.perform(get(PRODUCT_URL, PRODUCT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(PRODUCT_ID.toString()))
                    .andExpect(jsonPath("$.name").value("Galaxy cosmic milk"));
        }

        @Test
        void returns404ProblemWhenProductDoesNotExist() throws Exception {
            when(productService.getProductById(PRODUCT_ID)).thenThrow(new ProductNotFoundException(PRODUCT_ID));

            mockMvc.perform(get(PRODUCT_URL, PRODUCT_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.type").value(ERRORS_BASE_URL + "product-not-found"))
                    .andExpect(jsonPath("$.title").value("Product not found"))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.detail").value("Product with id '%s' was not found.".formatted(PRODUCT_ID)))
                    .andExpect(jsonPath("$.instance").value("/api/v1/products/" + PRODUCT_ID));
        }

        @Test
        void returns400WhenIdIsNotUuid() throws Exception {
            mockMvc.perform(get(PRODUCT_URL, "abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.instance").value("/api/v1/products/abc"));

            verifyNoInteractions(productService);
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/products/{id}")
    class UpdateProduct {

        @Test
        void takesIdFromUrlAndReturnsUpdatedProduct() throws Exception {
            when(productService.updateProduct(any())).thenAnswer(invocation -> invocation.getArgument(0));
            Map<String, Object> request = validRequest();
            request.put("id", UUID.randomUUID().toString());

            putProduct(PRODUCT_ID, request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(PRODUCT_ID.toString()))
                    .andExpect(jsonPath("$.name").value("Moon dust catnip"));

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productService).updateProduct(captor.capture());
            assertThat(captor.getValue().id()).isEqualTo(PRODUCT_ID);
        }

        @Test
        void rejectsInvalidBodyAndDoesNotCallService() throws Exception {
            putProduct(PRODUCT_ID, validRequestWith("price", -1))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("price"))
                    .andExpect(jsonPath("$.instance").value("/api/v1/products/" + PRODUCT_ID));

            verifyNoInteractions(productService);
        }

        @Test
        void returns404WhenProductDoesNotExist() throws Exception {
            when(productService.updateProduct(any())).thenThrow(new ProductNotFoundException(PRODUCT_ID));

            putProduct(PRODUCT_ID, validRequest())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value(ERRORS_BASE_URL + "product-not-found"));
        }

        @Test
        void returns409WhenNewNameBelongsToAnotherProduct() throws Exception {
            when(productService.updateProduct(any()))
                    .thenThrow(new ProductNameAlreadyExistsException("Galaxy cosmic milk"));

            putProduct(PRODUCT_ID, validRequestWith("name", "Galaxy cosmic milk"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/products/{id}")
    class DeleteProduct {

        @Test
        void returns204WithoutBody() throws Exception {
            mockMvc.perform(delete(PRODUCT_URL, PRODUCT_ID))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(productService).deleteProduct(PRODUCT_ID);
        }

        @Test
        void returns404WhenProductDoesNotExist() throws Exception {
            doThrow(new ProductNotFoundException(PRODUCT_ID)).when(productService).deleteProduct(PRODUCT_ID);

            mockMvc.perform(delete(PRODUCT_URL, PRODUCT_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value(ERRORS_BASE_URL + "product-not-found"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products/{id}/price")
    class GetProductPrice {

        private static final String PRICE_URL = "/api/v1/products/{id}/price";

        @Test
        void returnsConvertedPrice() throws Exception {
            when(productPriceService.getPrice(PRODUCT_ID, "EUR")).thenReturn(new ProductPrice(PRODUCT_ID,
                    new BigDecimal("12.50"), "USD", new BigDecimal("11.50"), "EUR", new BigDecimal("0.92")));

            mockMvc.perform(get(PRICE_URL, PRODUCT_ID).param("currency", "EUR"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.productId").value(PRODUCT_ID.toString()))
                    .andExpect(jsonPath("$.basePrice").value(12.50))
                    .andExpect(jsonPath("$.baseCurrency").value("USD"))
                    .andExpect(jsonPath("$.price").value(11.50))
                    .andExpect(jsonPath("$.currency").value("EUR"))
                    .andExpect(jsonPath("$.exchangeRate").value(0.92));
        }

        // @Pattern on a @RequestParam is checked by Spring MVC method validation without @Validated
        // on the controller (Spring Framework 6.1+), errors come as HandlerMethodValidationException
        @ParameterizedTest(name = "currency={0}")
        @ValueSource(strings = {"eur", "EURO", "E1R", "€"})
        void rejectsCurrencyThatIsNotIsoCode(String currency) throws Exception {
            mockMvc.perform(get(PRICE_URL, PRODUCT_ID).param("currency", currency))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value(ERRORS_BASE_URL + "validation"))
                    .andExpect(jsonPath("$.detail").value(
                            "Field 'currency' must be a 3-letter ISO 4217 code in upper case."))
                    .andExpect(jsonPath("$.errors[0].field").value("currency"));

            verifyNoInteractions(productPriceService);
        }

        @Test
        void rejectsMissingCurrency() throws Exception {
            mockMvc.perform(get(PRICE_URL, PRODUCT_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value("Required parameter 'currency' is not present."));

            verifyNoInteractions(productPriceService);
        }

        @Test
        void returns422ForCurrencyUnknownToExchangeService() throws Exception {
            when(productPriceService.getPrice(PRODUCT_ID, "XYZ")).thenThrow(new UnsupportedCurrencyException("XYZ"));

            mockMvc.perform(get(PRICE_URL, PRODUCT_ID).param("currency", "XYZ"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.type").value(ERRORS_BASE_URL + "currency-not-supported"))
                    .andExpect(jsonPath("$.title").value("Currency not supported"));
        }

        @Test
        void returns503WithoutDetailsOfExternalServiceFailure() throws Exception {
            when(productPriceService.getPrice(PRODUCT_ID, "EUR")).thenThrow(new ExchangeRateUnavailableException(
                    "call failed", new IllegalStateException("http://internal-rates:8090 refused")));

            mockMvc.perform(get(PRICE_URL, PRODUCT_ID).param("currency", "EUR"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.type").value(ERRORS_BASE_URL + "exchange-rate-unavailable"))
                    .andExpect(content().string(not(containsString("internal-rates"))));
        }
    }

    @Test
    void returns405ProblemForUnsupportedMethod() throws Exception {
        mockMvc.perform(patch(PRODUCT_URL, PRODUCT_ID).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    static Stream<Arguments> validRequestVariants() {
        return Stream.of(
                Arguments.of("name with cosmic word in upper case", "name", "GALAXY food bowl"),
                Arguments.of("name with hyphenated cosmic word", "name", "Star-shaped food bowl"),
                Arguments.of("name of exactly 100 characters", "name", "Star " + "x".repeat(95)),
                Arguments.of("name with plural cosmic word", "name", "Shooting stars wand"),
                Arguments.of("name with plural 'galaxies'", "name", "Two galaxies blanket"),
                Arguments.of("description is absent", "description", null),
                Arguments.of("description of exactly 500 characters", "description", "d".repeat(500)),
                Arguments.of("smallest price", "price", new BigDecimal("0.01")),
                Arguments.of("largest price", "price", new BigDecimal("9999999999.99")),
                Arguments.of("price with trailing zero", "price", new BigDecimal("1.50")),
                Arguments.of("zero stock", "stockQuantity", 0)
        );
    }

    static Stream<Arguments> invalidFieldValues() {
        return Stream.of(
                Arguments.of("name is missing", "name", null, "must not be blank"),
                Arguments.of("name without cosmic word", "name", "Yarn ball", "must contain at least one cosmic word "
                        + "(star, galaxy, comet, cosmic, space, planet, moon, nebula, orbit, asteroid)"),
                Arguments.of("cosmic word only as part of another word", "name", "Starch snack", "must contain at least "
                        + "one cosmic word (star, galaxy, comet, cosmic, space, planet, moon, nebula, orbit, asteroid)"),
                Arguments.of("name longer than 100 characters", "name", "Star " + "x".repeat(96),
                        "must be between 3 and 100 characters"),
                Arguments.of("description longer than 500 characters", "description", "d".repeat(501),
                        "must be at most 500 characters"),
                Arguments.of("price is missing", "price", null, "must not be null"),
                Arguments.of("price is zero", "price", BigDecimal.ZERO, "must be greater than 0"),
                Arguments.of("price is negative", "price", new BigDecimal("-5"), "must be greater than 0"),
                Arguments.of("price has 3 decimal places", "price", new BigDecimal("1.999"),
                        "must have at most 10 integer digits and 2 decimal places"),
                Arguments.of("price has 11 integer digits", "price", new BigDecimal("12345678901"),
                        "must have at most 10 integer digits and 2 decimal places"),
                Arguments.of("stockQuantity is missing", "stockQuantity", null, "must not be null"),
                Arguments.of("stockQuantity is negative", "stockQuantity", -1, "must be greater than or equal to 0"),
                Arguments.of("categoryId is missing", "categoryId", null, "must not be null")
        );
    }

    static Stream<Arguments> valuesOfWrongType() {
        return Stream.of(
                Arguments.of("categoryId is not UUID", "categoryId", "not-a-uuid"),
                Arguments.of("price is not a number", "price", "cheap"),
                Arguments.of("stockQuantity is fractional", "stockQuantity", 2.7),
                Arguments.of("stockQuantity is not a number", "stockQuantity", "many")
        );
    }

    private ResultActions postProduct(Map<String, Object> request) throws Exception {
        return mockMvc.perform(post(PRODUCTS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions putProduct(UUID id, Map<String, Object> request) throws Exception {
        return mockMvc.perform(put(PRODUCT_URL, id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private static Map<String, Object> validRequest() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("name", "Moon dust catnip");
        request.put("description", "Collected on the dark side");
        request.put("price", new BigDecimal("7.25"));
        request.put("stockQuantity", 50);
        request.put("categoryId", CATEGORY_ID.toString());
        return request;
    }

    private static Map<String, Object> validRequestWith(String field, Object value) {
        Map<String, Object> request = validRequest();
        if (value == null) {
            request.remove(field);
        } else {
            request.put(field, value);
        }
        return request;
    }

    private static Product product(UUID id, String name) {
        return new Product(id, name, null, new BigDecimal("4.99"), 300, CATEGORY_ID);
    }
}
