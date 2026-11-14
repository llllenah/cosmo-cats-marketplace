package com.cosmocats.marketplace.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The 403 of the filter chain cannot be reached over HTTP today (the chain only asks for
 * authentication, roles are checked on the services), so the handler is checked directly.
 */
@DisplayName("SecurityProblemHandler")
class SecurityProblemHandlerTest {

    // Built the same way as the ObjectMapper of the application, which knows how to write ProblemDetail
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private final SecurityProblemHandler handler = new SecurityProblemHandler(objectMapper);
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/reports/popular-products");
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @Test
    void writes403Problem() throws Exception {
        handler.handle(request, response, new AccessDeniedException("Access Denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/problem+json");
        JsonNode problem = objectMapper.readTree(response.getContentAsString());
        assertThat(problem.get("type").asText()).isEqualTo("https://cosmo-cats.market/errors/forbidden");
        assertThat(problem.get("title").asText()).isEqualTo("Access denied");
        assertThat(problem.get("status").asInt()).isEqualTo(403);
        assertThat(problem.get("detail").asText()).isEqualTo("You do not have permission to perform this action.");
        assertThat(problem.get("instance").asText()).isEqualTo("/api/v1/reports/popular-products");
    }

    @Test
    void writes401ProblemWithoutReasonOfFailure() throws Exception {
        handler.commence(request, response, new BadCredentialsException("API key and Authorization header in one request"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
        assertThat(response.getContentType()).isEqualTo("application/problem+json");
        JsonNode problem = objectMapper.readTree(response.getContentAsString());
        assertThat(problem.get("type").asText()).isEqualTo("https://cosmo-cats.market/errors/unauthorized");
        assertThat(problem.get("detail").asText()).isEqualTo("A valid bearer token or API key is required.");
        assertThat(problem.has("properties")).isFalse();
    }
}
