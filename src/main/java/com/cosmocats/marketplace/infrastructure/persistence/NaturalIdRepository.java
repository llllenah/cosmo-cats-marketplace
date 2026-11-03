package com.cosmocats.marketplace.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

/**
 * Repository for entities with a Hibernate {@code @NaturalId}.
 *
 * @param <T>  entity type
 * @param <ID> surrogate primary key type
 * @param <N>  natural id type
 */
@NoRepositoryBean
public interface NaturalIdRepository<T, ID, N> extends JpaRepository<T, ID> {

    Optional<T> findByNaturalId(N naturalId);
}
