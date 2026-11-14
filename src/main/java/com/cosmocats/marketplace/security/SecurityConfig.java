package com.cosmocats.marketplace.security;

import com.cosmocats.marketplace.security.apikey.ApiKeyAuthenticationFilter;
import com.cosmocats.marketplace.security.apikey.ApiKeyAuthenticationProvider;
import com.cosmocats.marketplace.security.apikey.ApiKeyProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Two filter chains, because the application has two kinds of callers:
 * <ul>
 *   <li>/api/** is called by programs. No session: every request brings a bearer token (JWT)
 *       or an API key.</li>
 *   <li>Everything else (Swagger UI, /account) is opened by people in a browser,
 *       who log in with GitHub and get a session.</li>
 * </ul>
 * The filter chains only decide who the caller is. What a role is allowed to do is written
 * next to the business logic with @PreAuthorize (see MethodSecurityConfig).
 */
@Configuration
@EnableWebSecurity
@Profile("!no-auth")
@EnableConfigurationProperties(ApiKeyProperties.class)
public class SecurityConfig {

    static final String ROLES_CLAIM = "roles";

    @Bean
    @Order(1)
    SecurityFilterChain apiFilterChain(HttpSecurity http, ApiKeyProperties apiKeyProperties,
                                       ObjectMapper objectMapper) throws Exception {
        SecurityProblemHandler problemHandler = new SecurityProblemHandler(objectMapper);
        ApiKeyAuthenticationFilter apiKeyFilter = new ApiKeyAuthenticationFilter(
                new ProviderManager(new ApiKeyAuthenticationProvider(apiKeyProperties)), problemHandler);

        http.securityMatcher("/api/**")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                // No session and no cookies, so there is nothing for CSRF to abuse
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(apiKeyFilter, BearerTokenAuthenticationFilter.class)
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(problemHandler)
                        .accessDeniedHandler(problemHandler)
                        .withObjectPostProcessor(failuresHandledBy(problemHandler)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problemHandler)
                        .accessDeniedHandler(problemHandler));
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain browserFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        // Spring Boot forwards failed requests here, the error itself is not a secret
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(Customizer.withDefaults());
        return http.build();
    }

    // The resource server DSL has no setter for the failure handler of its filter,
    // so it is set when the configurer has created the filter
    private static ObjectPostProcessor<BearerTokenAuthenticationFilter> failuresHandledBy(
            SecurityProblemHandler problemHandler) {
        return new ObjectPostProcessor<>() {
            @Override
            public <O extends BearerTokenAuthenticationFilter> O postProcess(O filter) {
                filter.setAuthenticationFailureHandler(problemHandler);
                return filter;
            }
        };
    }

    // By default Spring reads authorities from the "scope" claim. Here the identity provider
    // puts the roles of the user into "roles": ["ADMIN"] becomes ROLE_ADMIN.
    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter rolesConverter = new JwtGrantedAuthoritiesConverter();
        rolesConverter.setAuthoritiesClaimName(ROLES_CLAIM);
        rolesConverter.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(rolesConverter);
        return converter;
    }
}
