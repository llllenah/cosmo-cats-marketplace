package com.cosmocats.marketplace.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Profile for running the application on a developer machine without tokens, keys and GitHub:
 * {@code ./gradlew bootRun --args='--spring.profiles.active=no-auth'}. Every request is allowed.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@Profile("no-auth")
public class NoAuthSecurityConfig {

    private static final Profiles PRODUCTION = Profiles.of("prod", "production");

    @Bean
    SecurityFilterChain noAuthFilterChain(HttpSecurity http, Environment environment) throws Exception {
        if (environment.acceptsProfiles(PRODUCTION)) {
            throw new IllegalStateException("Profile 'no-auth' switches authentication off "
                    + "and must not be combined with a production profile");
        }
        log.warn("Profile 'no-auth' is active: every request is allowed without authentication. "
                + "Use it only on a local machine.");
        http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
