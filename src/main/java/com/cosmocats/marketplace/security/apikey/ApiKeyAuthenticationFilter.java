package com.cosmocats.marketplace.security.apikey;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates requests that carry an API key in the X-API-Key header.
 * Requests without the header go on unchanged, so the bearer token filter can handle them.
 * It is created inside the security configuration and is not a Spring bean on purpose:
 * Spring Boot would also register a Filter bean for every request outside the security chain.
 */
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-Key";

    private final AuthenticationManager authenticationManager;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getHeader(API_KEY_HEADER) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Authentication authenticated = authenticationManager.authenticate(
                    ApiKeyAuthenticationToken.unauthenticated(apiKeyOf(request)));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authenticated);
            SecurityContextHolder.setContext(context);
        } catch (AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, ex);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static String apiKeyOf(HttpServletRequest request) {
        // With two kinds of credentials it is not clear who the caller wants to be
        if (request.getHeader(HttpHeaders.AUTHORIZATION) != null) {
            throw new BadCredentialsException("API key and Authorization header in one request");
        }
        return request.getHeader(API_KEY_HEADER);
    }
}
