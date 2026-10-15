package com.cosmocats.marketplace.featuretoggle;

import com.cosmocats.marketplace.featuretoggle.FeatureToggleConfig.FeatureFlags;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FeatureToggleAspect")
class FeatureToggleAspectTest {

    @Test
    void runsAnnotatedMethodAndReturnsItsResultWhenFeatureIsEnabled() {
        MethodLevelToggles target = new MethodLevelToggles();

        MethodLevelToggles proxy = proxy(target, Map.of("cosmoCats", true));

        assertThat(proxy.cosmoCats()).isEqualTo("cats");
        assertThat(target.calls).containsExactly("cosmoCats");
    }

    @Test
    void doesNotRunAnnotatedMethodWhenFeatureIsDisabled() {
        MethodLevelToggles target = new MethodLevelToggles();

        MethodLevelToggles proxy = proxy(target, Map.of("cosmoCats", false));

        assertThatThrownBy(proxy::cosmoCats)
                .isInstanceOf(FeatureNotAvailableException.class)
                .hasMessage("Feature 'cosmoCats' is not available.");
        assertThat(target.calls).isEmpty();
    }

    @Test
    void doesNotTouchMethodsWithoutAnnotation() {
        MethodLevelToggles proxy = proxy(new MethodLevelToggles(), Map.of());

        assertThat(proxy.alwaysAvailable()).isEqualTo("always");
    }

    @Test
    void passesExceptionsOfTheMethodThrough() {
        MethodLevelToggles proxy = proxy(new MethodLevelToggles(), Map.of("cosmoCats", true));

        assertThatThrownBy(proxy::failingCosmoCats)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
    }

    @Test
    void appliesClassLevelToggleToEveryMethod() {
        ClassLevelToggle disabled = proxy(new ClassLevelToggle(), Map.of("kittyProducts", false));
        ClassLevelToggle enabled = proxy(new ClassLevelToggle(), Map.of("kittyProducts", true));

        assertThatThrownBy(disabled::first).isInstanceOf(FeatureNotAvailableException.class);
        assertThatThrownBy(disabled::second).isInstanceOf(FeatureNotAvailableException.class);
        assertThat(enabled.first()).isEqualTo("first");
        assertThat(enabled.second()).isEqualTo("second");
    }

    @Test
    void methodLevelToggleWinsOverClassLevel() {
        ClassLevelToggle proxy = proxy(new ClassLevelToggle(), Map.of("kittyProducts", false, "cosmoCats", true));

        assertThat(proxy.overriddenByMethod()).isEqualTo("method wins");
    }

    // Known limitation of proxy-based Spring AOP, kept as a test so it is not forgotten
    @Test
    void callFromInsideTheSameClassIsNotChecked() {
        MethodLevelToggles proxy = proxy(new MethodLevelToggles(), Map.of("cosmoCats", false));

        assertThat(proxy.callsCosmoCatsInternally()).isEqualTo("cats");
    }

    private static <T> T proxy(T target, Map<String, Boolean> flags) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAspect(new FeatureToggleAspect(new FeatureToggleService(new FeatureFlags(flags))));
        return factory.getProxy();
    }

    static class MethodLevelToggles {

        final List<String> calls = new ArrayList<>();

        @FeatureToggle(Feature.COSMO_CATS)
        public String cosmoCats() {
            calls.add("cosmoCats");
            return "cats";
        }

        @FeatureToggle(Feature.COSMO_CATS)
        public String failingCosmoCats() {
            throw new IllegalStateException("boom");
        }

        public String alwaysAvailable() {
            return "always";
        }

        public String callsCosmoCatsInternally() {
            return cosmoCats();
        }
    }

    @FeatureToggle(Feature.KITTY_PRODUCTS)
    static class ClassLevelToggle {

        public String first() {
            return "first";
        }

        public String second() {
            return "second";
        }

        @FeatureToggle(Feature.COSMO_CATS)
        public String overriddenByMethod() {
            return "method wins";
        }
    }
}
