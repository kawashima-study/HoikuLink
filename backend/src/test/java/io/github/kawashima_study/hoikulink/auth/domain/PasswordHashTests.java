package io.github.kawashima_study.hoikulink.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * パスワードハッシュ（PasswordHash）のテスト
 */
@DisplayName("パスワードハッシュ")
class PasswordHashTests {

    /** テストで使用するハッシュ値（本物のハッシュ値である必要はない） */
    private static final String HASH = "$argon2id$dummy";

    /**
     * 計算済みのハッシュ値をそのまま保持することを確認
     */
    @Test
    @DisplayName("計算済みのハッシュ値をそのまま保持する。")
    void keepsValue() {
        // ハッシュ値を渡して作成
        PasswordHash hash = new PasswordHash(HASH);

        // 取り出した値が渡した値と同じであることを確認
        assertThat(hash.value()).isEqualTo(HASH);
    }

    /**
     * null の場合に例外になることを確認
     */
    @Test
    @DisplayName("nullは受け付けない。")
    void rejectsNull() {
        // null を渡すと NullPointerException になることを確認
        assertThatThrownBy(() -> new PasswordHash(null)).isInstanceOf(NullPointerException.class);
    }

    /**
     * 空文字列または空白のみの場合に例外になることを確認
     *
     * @param value 空文字列または空白のみの値
     */
    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"   ", "\u3000", "\t"})
    @DisplayName("空文字列または空白のみの値は受け付けない。")
    void rejectsBlank(String value) {
        // 空文字列 半角の空白 全角の空白 タブを渡すと IllegalArgumentException になることを確認
        assertThatThrownBy(() -> new PasswordHash(value)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * toString の結果にハッシュ値が含まれないことを確認
     */
    @Test
    @DisplayName("toString() の結果にハッシュ値を含めない。")
    void doesNotRevealValueInToString() {
        // ハッシュ値を渡して作成
        PasswordHash hash = new PasswordHash(HASH);

        // ハッシュ値の一部が含まれていないことを確認
        assertThat(hash.toString()).doesNotContain("argon2id").doesNotContain("dummy");
    }
}
