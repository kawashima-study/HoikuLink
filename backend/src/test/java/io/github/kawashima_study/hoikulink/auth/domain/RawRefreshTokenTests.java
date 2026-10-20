package io.github.kawashima_study.hoikulink.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 生のリフレッシュトークン（RawRefreshToken）のテスト
 */
@DisplayName("生のリフレッシュトークン")
class RawRefreshTokenTests {

    /** 生のリフレッシュトークン（テストデータ） */
    private static final String REFRESH_TOKEN = "test-refresh-token";

    /**
     * 生のリフレッシュトークンをそのまま保持することを確認
     */
    @Test
    @DisplayName("生のリフレッシュトークンをそのまま保持する。")
    void keepsValue() {
        // 生のリフレッシュトークンを渡して作成
        RawRefreshToken refreshToken = new RawRefreshToken(REFRESH_TOKEN);

        // 取り出した値が渡した値と同じであることを確認
        assertThat(refreshToken.value()).isEqualTo(REFRESH_TOKEN);
    }

    /**
     * nullの場合に例外になることを確認
     */
    @Test
    @DisplayName("nullは受け付けない。")
    void rejectsNull() {
        // nullを渡すとNullPointerExceptionになることを確認
        assertThatThrownBy(() -> new RawRefreshToken(null)).isInstanceOf(NullPointerException.class);
    }

    /**
     * toStringの結果にリフレッシュトークンが含まれないことを確認
     */
    @Test
    @DisplayName("toString()の結果にリフレッシュトークンを含めない。")
    void doesNotRevealValueInToString() {
        // 生のリフレッシュトークンを渡して作成
        RawRefreshToken refreshToken = new RawRefreshToken(REFRESH_TOKEN);

        // リフレッシュトークンが含まれていないことを確認
        assertThat(refreshToken.toString()).doesNotContain(REFRESH_TOKEN);
    }
}
