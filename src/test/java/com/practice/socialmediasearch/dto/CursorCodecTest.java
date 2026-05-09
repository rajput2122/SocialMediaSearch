package com.practice.socialmediasearch.dto;

import com.practice.socialmediasearch.exception.BadCursorException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CursorCodecTest {

    @Test
    void encodeThenDecodeRoundTrips() {
        List<Object> sortValues = List.of(1.5, "abc-123");
        String cursor = CursorCodec.encode(sortValues);

        assertThat(cursor).isNotBlank();
        assertThat(CursorCodec.decode(cursor)).containsExactly(1.5, "abc-123");
    }

    @Test
    void encodeReturnsNullForNullOrEmpty() {
        assertThat(CursorCodec.encode(null)).isNull();
        assertThat(CursorCodec.encode(List.of())).isNull();
    }

    @Test
    void decodeReturnsNullForNullOrBlank() {
        assertThat(CursorCodec.decode(null)).isNull();
        assertThat(CursorCodec.decode("")).isNull();
        assertThat(CursorCodec.decode("   ")).isNull();
    }

    @Test
    void decodeThrowsBadCursorExceptionForInvalidBase64() {
        assertThatThrownBy(() -> CursorCodec.decode("not-valid-base64!!!"))
                .isInstanceOf(BadCursorException.class);
    }

    @Test
    void decodeThrowsBadCursorExceptionForBase64ButNotJson() {
        // valid base64 ("hello") but not a JSON list
        assertThatThrownBy(() -> CursorCodec.decode("aGVsbG8"))
                .isInstanceOf(BadCursorException.class);
    }

    @Test
    void cursorIsUrlSafe() {
        // cursor must be safe to pass as a URL query param (no '+', '/', or '=' padding)
        List<Object> longValues = List.of("a/b+c=", 1.0);
        String cursor = CursorCodec.encode(longValues);

        assertThat(cursor).doesNotContain("+", "/", "=");
    }
}
