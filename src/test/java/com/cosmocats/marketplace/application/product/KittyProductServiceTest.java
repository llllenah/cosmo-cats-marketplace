package com.cosmocats.marketplace.application.product;

import com.cosmocats.marketplace.domain.product.Product;
import com.cosmocats.marketplace.featuretoggle.FeatureNotAvailableException;
import com.cosmocats.marketplace.featuretoggle.FeatureToggleAspect;
import com.cosmocats.marketplace.featuretoggle.FeatureToggleConfig;
import com.cosmocats.marketplace.featuretoggle.FeatureToggleService;
import com.cosmocats.marketplace.infrastructure.persistence.EntityMapper;
import com.cosmocats.marketplace.infrastructure.persistence.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;
import java.util.UUID;

import static com.cosmocats.marketplace.infrastructure.persistence.TestEntities.category;
import static com.cosmocats.marketplace.infrastructure.persistence.TestEntities.product;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("KittyProductService behind the kittyProducts feature toggle (class level)")
class KittyProductServiceTest {

    private static final UUID KITTY_CATEGORY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final ProductRepository productRepository = mock(ProductRepository.class);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AopAutoConfiguration.class))
            .withUserConfiguration(FeatureToggleConfig.class, FeatureToggleService.class,
                    FeatureToggleAspect.class, KittyProductService.class, EntityMapper.class)
            .withBean(ProductRepository.class, () -> productRepository)
            .withPropertyValues("application.kitty-products.category-id=" + KITTY_CATEGORY_ID);

    @Test
    void returnsProductsOfConfiguredCategoryWhenFeatureIsEnabled() {
        UUID toyId = UUID.randomUUID();
        when(productRepository.findByCategoryReferenceOrderByNameAscReferenceAsc(KITTY_CATEGORY_ID)).thenReturn(List.of(
                product(toyId, "Star yarn ball", "10.00", 3, category(KITTY_CATEGORY_ID, "Anti-gravity toys"))));

        contextRunner.withPropertyValues("feature.kittyProducts.enabled=true").run(context ->
                assertThat(context.getBean(KittyProductService.class).getKittyProducts())
                        .extracting(Product::id, Product::name, Product::categoryId)
                        .containsExactly(tuple(toyId, "Star yarn ball", KITTY_CATEGORY_ID)));
    }

    @Test
    void throwsFeatureNotAvailableWhenFeatureIsDisabled() {
        contextRunner.withPropertyValues("feature.kittyProducts.enabled=false").run(context -> {
            assertThatThrownBy(() -> context.getBean(KittyProductService.class).getKittyProducts())
                    .isInstanceOf(FeatureNotAvailableException.class)
                    .hasMessage("Feature 'kittyProducts' is not available.");
            verifyNoInteractions(productRepository);
        });
    }
}
