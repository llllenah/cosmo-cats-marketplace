package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.infrastructure.persistence.entity.CosmoCatEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static com.cosmocats.marketplace.infrastructure.persistence.TestEntities.cosmoCat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CosmoCatRepository")
class CosmoCatRepositoryIT extends AbstractRepositoryIT {

    @Autowired
    private CosmoCatRepository cosmoCatRepository;

    @Test
    void returnsCatsSortedByName() {
        cosmoCatRepository.save(cosmoCat(UUID.randomUUID(), "Andromeda", "andromeda@galaxy.space"));
        flushAndClear();

        assertThat(cosmoCatRepository.findAllByOrderByNameAsc()).extracting(CosmoCatEntity::getName)
                .startsWith("Andromeda")
                .containsSubsequence("Luna", "Nova", "Orion");
    }

    @Test
    void createsReadsUpdatesAndDeletesCat() {
        UUID reference = UUID.randomUUID();
        CosmoCatEntity saved = cosmoCatRepository.save(cosmoCat(reference, "Comet", "comet@tail.space"));
        flushAndClear();

        CosmoCatEntity found = cosmoCatRepository.findByReference(reference).orElseThrow();
        assertThat(found.getEmail()).isEqualTo("comet@tail.space");
        found.setHomePlanet("Pluto");
        flushAndClear();
        assertThat(cosmoCatRepository.findByReference(reference)).get()
                .extracting(CosmoCatEntity::getHomePlanet).isEqualTo("Pluto");

        cosmoCatRepository.deleteById(saved.getId());
        flushAndClear();
        assertThat(cosmoCatRepository.findByReference(reference)).isEmpty();
    }

    @Test
    void rejectsSecondCatWithSameEmail() {
        assertThatThrownBy(() -> cosmoCatRepository.saveAndFlush(cosmoCat(UUID.randomUUID(), "Fake Luna", "luna@moon.space")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
