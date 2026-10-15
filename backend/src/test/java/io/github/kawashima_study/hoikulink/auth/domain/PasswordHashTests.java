package io.github.kawashima_study.hoikulink.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("パスワードハッシュ")
class PasswordHashTests {

    private static final String HASH = "$argon2id$dummy";

    @Test
    @DisplayName("計算済みのハッシュ値をそのまま保持する")
    void keepsValue() {
        // パスワードのハッシュ値を渡す
        PasswordHash hash = new PasswordHash(HASH);
        // 受け取ったハッシュ値のままか確認
        assertThat(hash.value()).isEqualTo(HASH);
    }

    @Test
    @DisplayName("nullは受け付けない")
    void rejectsNull() {
        assertThatThrownBy(() -> new PasswordHash(null)).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"   ", "\u3000", "\t"})
    @DisplayName("空文字列、空白のみ、タブの値は受け付けない")
    void rejectsBlank(String value) {
        assertThatThrownBy(() -> new PasswordHash(value)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("toString() の結果に、ハッシュ値を含めない")
    void doesNotRevealValueInToString() {
        PasswordHash hash = new PasswordHash(HASH);

        assertThat(hash.toString()).doesNotContain("argon2id").doesNotContain("dummy");
    }
}