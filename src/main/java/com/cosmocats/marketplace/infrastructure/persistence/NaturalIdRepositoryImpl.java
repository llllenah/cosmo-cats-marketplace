package com.cosmocats.marketplace.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import java.util.Optional;

/**
 * Base class for all repositories (see {@link JpaConfig}). Adds lookup by natural id through
 * the Hibernate API: the entity is cached in the session by its natural id, so repeated
 * lookups in one transaction do not go to the database.
 */
public class NaturalIdRepositoryImpl<T, ID, N> extends SimpleJpaRepository<T, ID>
        implements NaturalIdRepository<T, ID, N> {

    private final EntityManager entityManager;

    public NaturalIdRepositoryImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
    }

    @Override
    public Optional<T> findByNaturalId(N naturalId) {
        return entityManager.unwrap(Session.class)
                .bySimpleNaturalId(getDomainClass())
                .loadOptional(naturalId);
    }
}
