package com.cosmocats.marketplace.application.product;

import com.cosmocats.marketplace.domain.product.Product;
import com.cosmocats.marketplace.featuretoggle.FeatureNotAvailableException;
import com.cosmocats.marketplace.featuretoggle.FeatureToggleAspect;
import com.cosmocats.marketplace.featuretoggle.FeatureToggleConfig;
import com.cosmocats.marketplace.featuretoggle.FeatureToggleService;
import com.cosmocats.marketplace.infrastructure.persistence.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
                    FeatureToggleAspect.class, KittyProductService.class)
            .withBean(ProductRepository.class, () -> productRepository)
            .withPropertyValues("application.kitty-products.category-id=" + KITTY_CATEGORY_ID);

    @Test
    void returnsProductsOfConfiguredCategoryWhenFeatureIsEnabled() {
        List<Product> toys = List.of(new Product(UUID.randomUUID(), "Star yarn ball", null, BigDecimal.TEN, 3,
                KITTY_CATEGORY_ID));
        when(productRepository.findByCategoryId(KITTY_CATEGORY_ID)).thenReturn(toys);

        contextRunner.withPropertyValues("feature.kittyProducts.enabled=true").run(context ->
                assertThat(context.getBean(KittyProductService.class).getKittyProducts()).isEqualTo(toys));
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
