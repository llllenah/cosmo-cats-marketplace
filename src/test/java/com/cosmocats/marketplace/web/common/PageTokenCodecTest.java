package com.cosmocats.marketplace.web.common;

import com.cosmocats.marketplace.application.product.PageCursor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PageTokenCodec")
class PageTokenCodecTest {

    private static final UUID PRODUCT_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000003");

    private final PageTokenCodec codec = new PageTokenCodec();

    @Test
    void encodedTokenDecodesBackToSameCursor() {
        PageCursor cursor = new PageCursor("Comet tail scratching post", PRODUCT_ID);

        assertThat(codec.decode(codec.encode(cursor))).isEqualTo(cursor);
    }

    @Test
    void keepsSeparatorAndNonAsciiCharactersInName() {
        PageCursor cursor = new PageCursor("Star: зоряний м'ячик ☄", PRODUCT_ID);

        assertThat(codec.decode(codec.encode(cursor))).isEqualTo(cursor);
    }

    @Test
    void producesUrlSafeTokenWithoutPadding() {
        String token = codec.encode(new PageCursor("Moon?/+= ball", PRODUCT_ID));

        assertThat(token).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void encodesNullCursorAsNullToken() {
        assertThat(codec.encode(null)).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void treatsMissingTokenAsFirstPage(String token) {
        assertThat(codec.decode(token)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"not base64 !", "a+b/c"})
    void rejectsTokenThatIsNotUrlSafeBase64(String token) {
        assertThatThrownBy(() -> codec.decode(token)).isInstanceOf(InvalidPageTokenException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"no-separator", "not-a-uuid:Star", ":Star"})
    void rejectsTokenWithBrokenContent(String rawContent) {
        String token = Base64.getUrlEncoder().encodeToString(rawContent.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> codec.decode(token))
                .isInstanceOf(InvalidPageTokenException.class)
                .hasMessage("Page token is invalid. Request the first page without pageToken.");
    }
}
