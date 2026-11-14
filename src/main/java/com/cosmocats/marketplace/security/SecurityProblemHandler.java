package com.cosmocats.marketplace.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

import static com.cosmocats.marketplace.web.exception.ProblemFactory.problem;

/**
 * Writes 401 and 403 of the API as problem details. The security filters work before
 * the controllers, so GlobalExceptionHandler never sees these errors.
 */
@RequiredArgsConstructor
public class SecurityProblemHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    // Sets the 401 status and the WWW-Authenticate header from RFC 6750, with the reason for a bad token
    private final BearerTokenAuthenticationEntryPoint bearerTokenEntryPoint = new BearerTokenAuthenticationEntryPoint();
    private final ObjectMapper objectMapper;

    public static ProblemDetail unauthorized(String instance) {
        return problem(HttpStatus.UNAUTHORIZED, "unauthorized", "Authentication required",
                "A valid bearer token or API key is required.", instance);
    }

    public static ProblemDetail forbidden(String instance) {
        return problem(HttpStatus.FORBIDDEN, "forbidden", "Access denied",
                "You do not have permission to perform this action.", instance);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        bearerTokenEntryPoint.commence(request, response, authException);
        write(response, unauthorized(request.getRequestURI()));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        write(response, forbidden(request.getRequestURI()));
    }

    private void write(HttpServletResponse response, ProblemDetail problem) throws IOException {
        // No charset parameter: JSON is UTF-8 by definition, and the errors of the controllers
        // have exactly this media type too
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
