package io.github.kawashima_study.hoikulink.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.kawashima_study.hoikulink.shared.UserId;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * リフレッシュトークン（RefreshToken）のテスト
 */
@DisplayName("リフレッシュトークン")
class RefreshTokenTests {

    /** トークンを発行するアカウントのユーザーID（テストデータ） */
    private static final UserId USER_ID = UserId.newId();

    /** 生のリフレッシュトークンのハッシュ値（テストデータ） */
    private static final RefreshTokenHash TOKEN_HASH = RefreshTokenHash.of(new RawRefreshToken("abc"));

    /** 発行日時（テストデータ） */
    private static final Instant ISSUED_AT = Instant.parse("2026-10-18T01:00:00Z");

    /** 有効期限（発行日時の12時間後）本体と同じ計算をせず答えの日時をそのまま書く */
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-18T13:00:00Z");

    /**
     * 発行したトークンのIDがRTKで始まることを確認
     */
    @Test
    @DisplayName("発行したトークンのIDはRTKで始まる。")
    void issuedTokenIdStartsWithPrefix() {
        // トークンを発行
        RefreshToken token = RefreshToken.issue(USER_ID, TOKEN_HASH, ISSUED_AT);

        // IDがRTKで始まることを確認
        assertThat(token.refreshTokenId().value()).startsWith("RTK");
    }

    /**
     * ログインで発行したトークンのトークンファミリーIDが自分のIDと同じであることを確認
     */
    @Test
    @DisplayName("ログインで発行したトークンのトークンファミリーIDは自分のIDと同じになる。")
    void familyIdIsOwnId() {
        // トークンを発行
        RefreshToken token = RefreshToken.issue(USER_ID, TOKEN_HASH, ISSUED_AT);

        // トークンファミリーIDが自分のIDと同じであることを確認
        assertThat(token.familyId()).isEqualTo(token.refreshTokenId());
    }

    /**
     * 有効期限が発行日時の12時間後になることを確認
     */
    @Test
    @DisplayName("有効期限は発行日時の12時間後になる。")
    void expiresTwelveHoursAfterIssued() {
        // トークンを発行
        RefreshToken token = RefreshToken.issue(USER_ID, TOKEN_HASH, ISSUED_AT);

        // 有効期限が発行日時の12時間後であることを確認
        assertThat(token.expiresAt()).isEqualTo(EXPIRES_AT);
    }

    /**
     * 発行したときは無効化されていないことを確認
     */
    @Test
    @DisplayName("発行したときは無効化されていない。")
    void isNotRevokedWhenIssued() {
        // トークンを発行
        RefreshToken token = RefreshToken.issue(USER_ID, TOKEN_HASH, ISSUED_AT);

        // 無効化日時が空であることを確認
        assertThat(token.revokedAt()).isEmpty();
    }

    /**
     * ユーザーIDをそのまま保持することを確認
     */
    @Test
    @DisplayName("ユーザーIDをそのまま保持する。")
    void keepsUserId() {
        // トークンを発行
        RefreshToken token = RefreshToken.issue(USER_ID, TOKEN_HASH, ISSUED_AT);

        // 取り出したユーザーIDが渡した値と同じであることを確認
        assertThat(token.userId()).isEqualTo(USER_ID);
    }

    /**
     * ユーザーIDがnullの場合に例外になることを確認
     */
    @Test
    @DisplayName("ユーザーIDのnullは受け付けない。")
    void rejectsNullUserId() {
        // ユーザーIDにnullを渡すとNullPointerExceptionになることを確認
        assertThatThrownBy(() -> RefreshToken.issue(null, TOKEN_HASH, ISSUED_AT))
                .isInstanceOf(NullPointerException.class);
    }

    /**
     * ハッシュ値がnullの場合に例外になることを確認
     */
    @Test
    @DisplayName("ハッシュ値のnullは受け付けない。")
    void rejectsNullTokenHash() {
        // ハッシュ値にnullを渡すとNullPointerExceptionになることを確認
        assertThatThrownBy(() -> RefreshToken.issue(USER_ID, null, ISSUED_AT)).isInstanceOf(NullPointerException.class);
    }

    /**
     * 発行日時がnullの場合に例外になることを確認
     */
    @Test
    @DisplayName("発行日時のnullは受け付けない。")
    void rejectsNullIssuedAt() {
        // 発行日時にnullを渡すとNullPointerExceptionになることを確認
        assertThatThrownBy(() -> RefreshToken.issue(USER_ID, TOKEN_HASH, null))
                .isInstanceOf(NullPointerException.class);
    }
}
