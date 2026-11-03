package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.infrastructure.persistence.entity.CategoryEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static com.cosmocats.marketplace.infrastructure.persistence.TestEntities.category;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CategoryRepository")
class CategoryRepositoryIT extends AbstractRepositoryIT {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void createsReadsUpdatesAndDeletesCategory() {
        UUID reference = UUID.randomUUID();
        CategoryEntity saved = categoryRepository.save(category(reference, "Asteroid snacks"));
        flushAndClear();
        assertThat(saved.getId()).isNotNull();

        CategoryEntity found = categoryRepository.findByReference(reference).orElseThrow();
        assertThat(found.getName()).isEqualTo("Asteroid snacks");

        found.setName("Asteroid treats");
        flushAndClear();
        assertThat(categoryRepository.findByReference(reference)).get()
                .extracting(CategoryEntity::getName).isEqualTo("Asteroid treats");

        categoryRepository.deleteById(saved.getId());
        flushAndClear();
        assertThat(categoryRepository.existsByReference(reference)).isFalse();
    }

    @Test
    void findsSeededCategoryByPublicId() {
        UUID cosmicFood = UUID.fromString("22222222-2222-2222-2222-222222222222");

        assertThat(categoryRepository.existsByReference(cosmicFood)).isTrue();
        assertThat(categoryRepository.findByReference(cosmicFood)).get()
                .extracting(CategoryEntity::getName).isEqualTo("Cosmic food");
        assertThat(categoryRepository.findByReference(UUID.randomUUID())).isEmpty();
    }

    @Test
    void rejectsSecondCategoryWithSameName() {
        assertThatThrownBy(() -> categoryRepository.saveAndFlush(category(UUID.randomUUID(), "Cosmic food")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
