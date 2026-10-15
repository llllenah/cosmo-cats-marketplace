package com.cosmocats.marketplace.web.exception;

import com.cosmocats.marketplace.application.exception.ProductValidationException;
import com.cosmocats.marketplace.application.exception.ProductValidationException.Violation;
import com.cosmocats.marketplace.web.exception.GlobalExceptionHandler.FieldValidationError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Over HTTP Bean Validation rejects a request before ProductMapper can throw
 * ProductValidationException, so this handler is checked directly.
 */
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void convertsProductValidationExceptionToSortedValidationProblem() {
        ProductValidationException ex = new ProductValidationException(List.of(
                new Violation("price", "must be greater than 0"),
                new Violation("name", "must not be blank")));

        ProblemDetail problem = handler.handleProductValidation(ex, new MockHttpServletRequest("POST", "/api/v1/products"));

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getType()).isEqualTo(URI.create("https://cosmo-cats.market/errors/validation"));
        assertThat(problem.getInstance()).isEqualTo(URI.create("/api/v1/products"));
        assertThat(problem.getDetail()).isEqualTo("Field 'name' must not be blank. Field 'price' must be greater than 0.");
        assertThat(problem.getProperties()).containsEntry("errors", List.of(
                new FieldValidationError("name", "must not be blank"),
                new FieldValidationError("price", "must be greater than 0")));
    }
}
