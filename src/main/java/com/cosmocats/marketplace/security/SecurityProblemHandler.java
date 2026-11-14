package com.cosmocats.marketplace.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

import static com.cosmocats.marketplace.web.exception.ProblemFactory.problem;

/**
 * Writes the security errors of the API as problem details. The security filters work before
 * the controllers, so GlobalExceptionHandler never sees these errors.
 */
@Slf4j
@RequiredArgsConstructor
public class SecurityProblemHandler
        implements AuthenticationEntryPoint, AccessDeniedHandler, AuthenticationFailureHandler {

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

    public static ProblemDetail authenticationUnavailable(String instance) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "authentication-unavailable", "Authentication unavailable",
                "Access tokens cannot be checked right now. Please try again later.", instance);
    }

    /**
     * Failure of the bearer token filter. A token that was checked and rejected is the fault of
     * the caller (401). AuthenticationServiceException means the token could not be checked at all,
     * e.g. the keys of the identity provider cannot be downloaded: by default the filter rethrows it
     * and the caller gets 500 for what may be a perfectly valid token.
     */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        if (exception instanceof AuthenticationServiceException) {
            log.error("Cannot check the access token of {} {}", request.getMethod(), request.getRequestURI(), exception);
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            write(response, authenticationUnavailable(request.getRequestURI()));
            return;
        }
        commence(request, response, exception);
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
