package com.cosmocats.marketplace.infrastructure.persistence;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackageClasses = JpaConfig.class, repositoryBaseClass = NaturalIdRepositoryImpl.class)
public class JpaConfig {
}
