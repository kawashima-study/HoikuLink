package io.github.kawashima_study.hoikulink.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * アクセストークン（AccessToken）のテスト
 */
@DisplayName("アクセストークン")
class AccessTokenTests {

    /** アクセストークン（テストデータ） */
    private static final String ACCESS_TOKEN = "test-access-token";

    /** 有効期限までの時間（テストデータ） */
    private static final Duration EXPIRES_IN = Duration.ofMinutes(5);

    /**
     * アクセストークンをそのまま保持することを確認
     */
    @Test
    @DisplayName("アクセストークンをそのまま保持する。")
    void keepsValue() {
        // アクセストークンと有効期限までの時間を渡して作成
        AccessToken accessToken = new AccessToken(ACCESS_TOKEN, EXPIRES_IN);

        // 取り出したアクセストークンが渡した値と同じであることを確認
        assertThat(accessToken.value()).isEqualTo(ACCESS_TOKEN);
    }

    /**
     * 有効期限までの時間をそのまま保持することを確認
     */
    @Test
    @DisplayName("有効期限までの時間をそのまま保持する。")
    void keepsExpiresIn() {
        // アクセストークンと有効期限までの時間を渡して作成
        AccessToken accessToken = new AccessToken(ACCESS_TOKEN, EXPIRES_IN);

        // 取り出した有効期限までの時間が渡した値と同じであることを確認
        assertThat(accessToken.expiresIn()).isEqualTo(EXPIRES_IN);
    }

    /**
     * アクセストークンがnullの場合に例外になることを確認
     */
    @Test
    @DisplayName("アクセストークンのnullは受け付けない。")
    void rejectsNullValue() {
        // アクセストークンにnullを渡すとNullPointerExceptionになることを確認
        assertThatThrownBy(() -> new AccessToken(null, EXPIRES_IN)).isInstanceOf(NullPointerException.class);
    }

    /**
     * 有効期限までの時間がnullの場合に例外になることを確認
     */
    @Test
    @DisplayName("有効期限までの時間のnullは受け付けない。")
    void rejectsNullExpiresIn() {
        // 有効期限までの時間にnullを渡すとNullPointerExceptionになることを確認
        assertThatThrownBy(() -> new AccessToken(ACCESS_TOKEN, null)).isInstanceOf(NullPointerException.class);
    }

    /**
     * toStringの結果にアクセストークンが含まれないことを確認
     */
    @Test
    @DisplayName("toString()の結果にアクセストークンを含めない。")
    void doesNotRevealValueInToString() {
        // アクセストークンと有効期限までの時間を渡して作成
        AccessToken accessToken = new AccessToken(ACCESS_TOKEN, EXPIRES_IN);

        // アクセストークンが含まれていないことを確認
        assertThat(accessToken.toString()).doesNotContain(ACCESS_TOKEN);
    }
}
