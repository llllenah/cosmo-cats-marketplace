package com.cosmocats.marketplace.web;

import com.cosmocats.marketplace.AbstractIT;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.RequestBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Several requests at the same moment. The checks in the services are not enough here:
 * all requests pass them before any of them commits, the database constraints and
 * optimistic locking have to catch the rest, and the client must get 409, not 500.
 */
@DisplayName("Concurrent requests")
class ConcurrentRequestsIT extends AbstractIT {

    private static final int REQUESTS = 6;
    private static final String SPACE_GEAR_ID = "33333333-3333-3333-3333-333333333333";
    private static final String LUNA_ID = "cccccccc-0000-0000-0000-000000000001";

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sameProductCreatedAtOnceIsSavedOnlyOnce() throws Exception {
        String name = "Concurrent star " + UUID.randomUUID();

        List<MockHttpServletResponse> responses = sendAtOnce(() -> post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson(name, 10)));

        assertThat(statuses(responses)).containsOnly(201, 409).containsOnlyOnce(201);
    }

    @Test
    void lastItemInStockIsSoldOnlyOnce() throws Exception {
        String productId = objectMapper.readTree(mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Last comet " + UUID.randomUUID(), 1)))
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        List<MockHttpServletResponse> responses = sendAtOnce(() -> post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"cartId": "%s", "customerId": "%s", "items": [{"productId": "%s", "quantity": 1}]}
                        """.formatted(UUID.randomUUID(), LUNA_ID, productId)));

        assertThat(statuses(responses)).containsOnly(201, 409).containsOnlyOnce(201);
        String product = mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(product).get("stockQuantity").asInt()).isZero();
    }

    private List<MockHttpServletResponse> sendAtOnce(Supplier<RequestBuilder> request) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(REQUESTS);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<MockHttpServletResponse>> futures = new ArrayList<>();
            for (int i = 0; i < REQUESTS; i++) {
                Callable<MockHttpServletResponse> call = () -> {
                    start.await();
                    return mockMvc.perform(request.get()).andReturn().getResponse();
                };
                futures.add(executor.submit(call));
            }
            start.countDown();
            List<MockHttpServletResponse> responses = new ArrayList<>();
            for (Future<MockHttpServletResponse> future : futures) {
                responses.add(future.get());
            }
            return responses;
        } finally {
            executor.shutdownNow();
        }
    }

    private static List<Integer> statuses(List<MockHttpServletResponse> responses) {
        return responses.stream().map(MockHttpServletResponse::getStatus).toList();
    }

    private static String productJson(String name, int stock) {
        return """
                {"name": "%s", "price": 5.00, "stockQuantity": %d, "categoryId": "%s"}
                """.formatted(name, stock, SPACE_GEAR_ID);
    }
}
