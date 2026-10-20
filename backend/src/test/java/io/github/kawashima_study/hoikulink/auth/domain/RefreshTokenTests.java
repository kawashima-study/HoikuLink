package io.github.kawashima_study.hoikulink.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.kawashima_study.hoikulink.shared.UserId;
import java.time.Instant;
import java.util.Optional;
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

    /** 交換後の生のリフレッシュトークンのハッシュ値（テストデータ） */
    private static final RefreshTokenHash NEW_TOKEN_HASH = RefreshTokenHash.of(new RawRefreshToken("def"));

    /** 発行日時（テストデータ） */
    private static final Instant ISSUED_AT = Instant.parse("2026-10-18T01:00:00Z");

    /** 有効期限（発行日時の12時間後）本体と同じ計算をせず答えの日時をそのまま書く */
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-18T13:00:00Z");

    /** 交換日時（有効期限の前） */
    private static final Instant ROTATED_AT = Instant.parse("2026-10-18T02:00:00Z");

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
     * ユーザーIDとハッシュ値をそのまま保持することを確認
     */
    @Test
    @DisplayName("ユーザーIDとハッシュ値をそのまま保持する。")
    void keepsUserIdAndTokenHash() {
        // トークンを発行
        RefreshToken token = RefreshToken.issue(USER_ID, TOKEN_HASH, ISSUED_AT);

        // 取り出したユーザーIDとハッシュ値が渡した値と同じであることを確認
        assertThat(token.userId()).isEqualTo(USER_ID);
        assertThat(token.tokenHash()).isEqualTo(TOKEN_HASH);
    }

    /**
     * 交換すると同じトークンファミリーと有効期限の新しいトークンを返すことを確認
     */
    @Test
    @DisplayName("交換すると同じトークンファミリーと有効期限の新しいトークンを返す。")
    void rotateReturnsNewTokenInSameFamily() {
        // トークンを発行して交換
        RefreshToken current = RefreshToken.issue(USER_ID, TOKEN_HASH, ISSUED_AT);
        RefreshToken next = current.rotate(NEW_TOKEN_HASH, ROTATED_AT);

        // IDとハッシュ値は新しくトークンファミリーと有効期限は引き継いでいることを確認
        assertThat(next.refreshTokenId()).isNotEqualTo(current.refreshTokenId());
        assertThat(next.tokenHash()).isEqualTo(NEW_TOKEN_HASH);
        assertThat(next.familyId()).isEqualTo(current.familyId());
        assertThat(next.expiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(next.revokedAt()).isEmpty();
    }

    /**
     * 交換すると元のトークンを交換日時で無効化することを確認
     */
    @Test
    @DisplayName("交換すると元のトークンを交換日時で無効化する。")
    void rotateRevokesCurrentToken() {
        // トークンを発行して交換
        RefreshToken current = RefreshToken.issue(USER_ID, TOKEN_HASH, ISSUED_AT);
        current.rotate(NEW_TOKEN_HASH, ROTATED_AT);

        // 元のトークンの無効化日時が交換日時であることを確認
        assertThat(current.revokedAt()).isEqualTo(Optional.of(ROTATED_AT));
        assertThat(current.isRevoked()).isTrue();
    }

    /**
     * 無効化済みのトークンは交換できないことを確認
     */
    @Test
    @DisplayName("無効化済みのトークンは交換できない。")
    void rejectsRotationOfRevokedToken() {
        // 1回交換して無効化済みにする
        RefreshToken current = RefreshToken.issue(USER_ID, TOKEN_HASH, ISSUED_AT);
        current.rotate(NEW_TOKEN_HASH, ROTATED_AT);

        // もう一度交換すると例外になることを確認
        assertThatThrownBy(() -> current.rotate(NEW_TOKEN_HASH, ROTATED_AT)).isInstanceOf(IllegalStateException.class);
    }

    /**
     * 有効期限ちょうどのトークンは使えないことを確認
     */
    @Test
    @DisplayName("有効期限ちょうどになると使えない。")
    void isNotUsableAtExpiresAt() {
        // トークンを発行
        RefreshToken token = RefreshToken.issue(USER_ID, TOKEN_HASH, ISSUED_AT);

        // 有効期限の直前は使え有効期限ちょうどは使えないことを確認
        assertThat(token.isUsable(EXPIRES_AT.minusSeconds(1))).isTrue();
        assertThat(token.isUsable(EXPIRES_AT)).isFalse();
    }

    /**
     * 有効期限切れのトークンは交換できないことを確認
     */
    @Test
    @DisplayName("有効期限切れのトークンは交換できない。")
    void rejectsRotationOfExpiredToken() {
        // トークンを発行
        RefreshToken token = RefreshToken.issue(USER_ID, TOKEN_HASH, ISSUED_AT);

        // 有効期限ちょうどに交換すると例外になることを確認
        assertThatThrownBy(() -> token.rotate(NEW_TOKEN_HASH, EXPIRES_AT)).isInstanceOf(IllegalStateException.class);
    }

    /**
     * 保存されている値から作ると無効化日時も復元することを確認
     */
    @Test
    @DisplayName("保存されている値から作ると無効化日時も復元する。")
    void restoresRevokedAt() {
        // 無効化済みの値から作成
        RefreshTokenId id = RefreshTokenId.newId();
        RefreshToken token = new RefreshToken(id, USER_ID, TOKEN_HASH, id, EXPIRES_AT, ROTATED_AT);

        // 無効化済みとして復元されていることを確認
        assertThat(token.isRevoked()).isTrue();
        assertThat(token.revokedAt()).isEqualTo(Optional.of(ROTATED_AT));
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
