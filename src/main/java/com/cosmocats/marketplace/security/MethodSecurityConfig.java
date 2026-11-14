package com.cosmocats.marketplace.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Turns on @PreAuthorize and @PostAuthorize on the services. With the no-auth profile there is
 * no user to check, so the annotations are switched off together with authentication.
 */
@Configuration
@EnableMethodSecurity
@Profile("!no-auth")
public class MethodSecurityConfig {
}
