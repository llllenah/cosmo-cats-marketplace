package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.PostgresTestContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for repository tests: only the JPA part of the context (entities, repositories,
 * Liquibase) against PostgreSQL in Docker. Every test runs in a transaction that is rolled back,
 * so tests do not see each other's data.
 * JpaConfig is imported explicitly: @DataJpaTest skips @Configuration classes, and without it
 * the repositories would not get NaturalIdRepositoryImpl as the base class.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
public abstract class AbstractRepositoryIT {

    @Autowired
    protected TestEntityManager entityManager;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        PostgresTestContainer.registerDataSource(registry);
    }

    /**
     * Writes pending changes and forgets loaded entities, so the next read goes to the database.
     */
    protected void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
