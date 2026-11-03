package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.infrastructure.persistence.entity.CosmoCatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CosmoCatRepository extends JpaRepository<CosmoCatEntity, Long> {

    List<CosmoCatEntity> findAllByOrderByNameAsc();

    Optional<CosmoCatEntity> findByReference(UUID reference);
}
