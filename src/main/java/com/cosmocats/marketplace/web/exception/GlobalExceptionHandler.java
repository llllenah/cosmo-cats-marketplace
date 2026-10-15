package com.cosmocats.marketplace.web.exception;

import com.cosmocats.marketplace.application.exception.CategoryNotFoundException;
import com.cosmocats.marketplace.application.exception.ExchangeRateUnavailableException;
import com.cosmocats.marketplace.application.exception.ProductNameAlreadyExistsException;
import com.cosmocats.marketplace.application.exception.ProductNotFoundException;
import com.cosmocats.marketplace.application.exception.ProductValidationException;
import com.cosmocats.marketplace.application.exception.UnsupportedCurrencyException;
import com.cosmocats.marketplace.featuretoggle.FeatureNotAvailableException;
import com.cosmocats.marketplace.web.common.InvalidPageTokenException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Converts exceptions into RFC 9457 problem details.
 * Standard Spring MVC errors (405, 415, unknown path, wrong parameter type, etc.)
 * are handled by the parent class.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String ERROR_TYPE_BASE_URL = "https://cosmo-cats.market/errors/";

    // Bean Validation returns violations from a HashSet, so one field with several errors
    // came back in random order. Sorting by message too makes the response stable.
    private static final Comparator<FieldValidationError> ERROR_ORDER = Comparator
            .comparing(FieldValidationError::field)
            .thenComparing(FieldValidationError::message);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<FieldValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldValidationError(error.getField(), fieldErrorMessage(error)))
                .sorted(ERROR_ORDER)
                .toList();
        return ResponseEntity.badRequest().body(validationProblem(errors, requestUri(request)));
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        List<FieldValidationError> errors = ex.getAllValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldValidationError(
                                result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .sorted(ERROR_ORDER)
                .toList();
        return ResponseEntity.badRequest().body(validationProblem(errors, requestUri(request)));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        // Valid JSON with a value of the wrong type, e.g. "categoryId": "abc" or "stockQuantity": 2.7
        if (ex.getCause() instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
            List<FieldValidationError> errors = List.of(new FieldValidationError(jsonPath(mismatch), "has invalid value"));
            return ResponseEntity.badRequest().body(validationProblem(errors, requestUri(request)));
        }
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "malformed-request", "Malformed request body",
                "Request body is missing or is not valid JSON.", requestUri(request));
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ProductValidationException.class)
    public ProblemDetail handleProductValidation(ProductValidationException ex, HttpServletRequest request) {
        List<FieldValidationError> errors = ex.getViolations().stream()
                .map(violation -> new FieldValidationError(violation.field(), violation.message()))
                .sorted(ERROR_ORDER)
                .toList();
        return validationProblem(errors, request.getRequestURI());
    }

    @ExceptionHandler(InvalidPageTokenException.class)
    public ProblemDetail handleInvalidPageToken(InvalidPageTokenException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "invalid-page-token", "Invalid page token",
                ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFound(ProductNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "product-not-found", "Product not found",
                ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ProblemDetail handleCategoryNotFound(CategoryNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "category-not-found", "Category not found",
                ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ProductNameAlreadyExistsException.class)
    public ProblemDetail handleDuplicateProductName(ProductNameAlreadyExistsException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "product-already-exists", "Product already exists",
                ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(UnsupportedCurrencyException.class)
    public ProblemDetail handleUnsupportedCurrency(UnsupportedCurrencyException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "currency-not-supported", "Currency not supported",
                ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ExchangeRateUnavailableException.class)
    public ProblemDetail handleExchangeRateUnavailable(ExchangeRateUnavailableException ex, HttpServletRequest request) {
        // The cause may contain URLs and messages of the external service, so it goes to the log only
        log.warn("Exchange rate is unavailable for {}", request.getRequestURI(), ex);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "exchange-rate-unavailable", "Exchange rate unavailable",
                "Exchange rates are temporarily unavailable. Please try again later.", request.getRequestURI());
    }

    // For clients a switched off feature looks like an endpoint that does not exist yet
    @ExceptionHandler(FeatureNotAvailableException.class)
    public ProblemDetail handleFeatureNotAvailable(FeatureNotAvailableException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "feature-not-available", "Feature not available",
                ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error while processing {} {}", request.getMethod(), request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Internal server error",
                "Something went wrong on our side. Please try again later.", request.getRequestURI());
    }

    private static String jsonPath(MismatchedInputException ex) {
        return ex.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("."));
    }

    private static String fieldErrorMessage(FieldError error) {
        // Binding failure means the value could not be converted, e.g. pageSize=abc.
        return error.isBindingFailure() ? "has invalid value" : error.getDefaultMessage();
    }

    private static ProblemDetail validationProblem(List<FieldValidationError> errors, String instance) {
        String detail = errors.stream()
                .map(error -> "Field '%s' %s.".formatted(error.field(), error.message()))
                .collect(Collectors.joining(" "));
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "validation", "Validation failed", detail, instance);
        problem.setProperty("errors", errors);
        return problem;
    }

    private static ProblemDetail problem(HttpStatus status, String errorType, String title,
                                         String detail, String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(ERROR_TYPE_BASE_URL + errorType));
        problem.setTitle(title);
        problem.setInstance(URI.create(instance));
        return problem;
    }

    private static String requestUri(WebRequest request) {
        return ((ServletWebRequest) request).getRequest().getRequestURI();
    }

    public record FieldValidationError(String field, String message) {
    }
}
