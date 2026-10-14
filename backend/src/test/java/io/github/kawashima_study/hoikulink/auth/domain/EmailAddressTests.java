package io.github.kawashima_study.hoikulink.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("メールアドレス（ログインID）")
class EmailAddressTests {

    @Test
    @DisplayName("前後の空白を取り除き、小文字にそろえる")
    void normalizesToLowerCase() {
        assertThat(new EmailAddress("  Hanako@Example.COM ").value()).isEqualTo("hanako@example.com");
    }

    @Test
    @DisplayName("大文字・小文字だけが違うメールアドレスは、同じとみなす")
    void treatsCaseInsensitiveAddressesAsEqual() {
        assertThat(new EmailAddress("Hanako@Example.com")).isEqualTo(new EmailAddress("hanako@example.com"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "hanako", "hanako@", "@example.com", "hana ko@example.com", "hanako@example", "a@b@example.com"})
    @DisplayName("メールアドレスの形でない値は受け付けない")
    void rejectsInvalidAddress(String value) {
        assertThatThrownBy(() -> new EmailAddress(value)).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("254文字を超えるメールアドレスは受け付けない")
    void rejectsTooLongAddress() {
        String tooLong = "a".repeat(243) + "@example.com";

        assertThatThrownBy(() -> new EmailAddress(tooLong)).isInstanceOf(IllegalArgumentException.class);
    }
}
