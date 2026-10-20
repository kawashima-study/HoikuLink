package io.github.kawashima_study.hoikulink.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.kawashima_study.hoikulink.auth.domain.AccessToken;
import io.github.kawashima_study.hoikulink.auth.domain.AccessTokenIssuer;
import io.github.kawashima_study.hoikulink.auth.domain.RawRefreshToken;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshToken;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshTokenGenerator;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshTokenHash;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshTokenId;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshTokenRepository;
import io.github.kawashima_study.hoikulink.shared.BusinessTimeZone;
import io.github.kawashima_study.hoikulink.shared.UserId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * アクセストークンの再発行のユースケース（RefreshService）のテスト
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("アクセストークンの再発行のユースケース")
class RefreshServiceTests {

    /** 現在の時刻（テストデータ） */
    private static final Instant NOW = Instant.parse("2026-10-20T03:00:00Z");

    /** ログインした日時（テストデータ） */
    private static final Instant ISSUED_AT = Instant.parse("2026-10-20T01:00:00Z");

    /** リフレッシュトークンの有効期限（ログインの12時間後） */
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-20T13:00:00Z");

    /** アカウントのユーザーID（テストデータ） */
    private static final UserId USER_ID = UserId.newId();

    /** Cookieに入っていた生のリフレッシュトークン（テストデータ） */
    private static final String CURRENT_TOKEN = "current-refresh-token";

    /** 交換で作る新しい生のリフレッシュトークン（テストデータ） */
    private static final RawRefreshToken NEW_TOKEN = new RawRefreshToken("new-refresh-token");

    /** 発行するアクセストークン（テストデータ） */
    private static final AccessToken ACCESS_TOKEN = new AccessToken("test-access-token", Duration.ofMinutes(5));

    /** リフレッシュトークンの保存と読み出しの仮の部品 */
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    /** 生のリフレッシュトークンの作成の仮の部品 */
    @Mock
    private RefreshTokenGenerator refreshTokenGenerator;

    /** アクセストークンの発行の仮の部品 */
    @Mock
    private AccessTokenIssuer accessTokenIssuer;

    /** テスト対象 */
    private RefreshService refreshService;

    /**
     * 各テストの前にテスト対象を作成
     */
    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, BusinessTimeZone.JAPAN);
        refreshService = new RefreshService(refreshTokenRepository, refreshTokenGenerator, accessTokenIssuer, clock);
    }

    /**
     * 使えるトークンの場合に新しいトークンと交換しアクセストークンを返すことを確認
     */
    @Test
    @DisplayName("使えるトークンなら新しいトークンと交換しアクセストークンを返す。")
    void returnsSuccessWithRotatedToken() {
        // 使えるトークンと新しいトークンの作成とアクセストークンの発行を用意
        givenStoredToken(RefreshToken.issue(USER_ID, hashOf(CURRENT_TOKEN), ISSUED_AT));
        when(refreshTokenGenerator.generate()).thenReturn(NEW_TOKEN);
        when(accessTokenIssuer.issue(USER_ID, NOW)).thenReturn(ACCESS_TOKEN);

        // 再発行
        RefreshResult result = refreshService.refresh(new RefreshCommand(CURRENT_TOKEN));

        // 新しいトークンと延長されない有効期限を持つ成功の結果であることを確認
        assertThat(result).isEqualTo(new RefreshResult.Success(ACCESS_TOKEN, NEW_TOKEN, EXPIRES_AT));
    }

    /**
     * 交換すると元のトークンの無効化と新しいトークンの登録を書き込むことを確認
     */
    @Test
    @DisplayName("交換すると元のトークンの無効化と新しいトークンの登録を書き込む。")
    void writesRotation() {
        // 使えるトークンを用意
        RefreshToken current = RefreshToken.issue(USER_ID, hashOf(CURRENT_TOKEN), ISSUED_AT);
        givenStoredToken(current);
        when(refreshTokenGenerator.generate()).thenReturn(NEW_TOKEN);
        when(accessTokenIssuer.issue(USER_ID, NOW)).thenReturn(ACCESS_TOKEN);

        // 再発行
        refreshService.refresh(new RefreshCommand(CURRENT_TOKEN));

        // 元のトークンを無効化して書き込み同じトークンファミリーの新しいトークンを登録したことを確認
        assertThat(current.isRevoked()).isTrue();
        verify(refreshTokenRepository).update(current, USER_ID, NOW);
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture(), eq(USER_ID), eq(NOW));
        assertThat(captor.getValue().tokenHash()).isEqualTo(RefreshTokenHash.of(NEW_TOKEN));
        assertThat(captor.getValue().familyId()).isEqualTo(current.familyId());
    }

    /**
     * トークンが見つからない場合に再発行できないことを確認
     */
    @Test
    @DisplayName("トークンが見つからなければ再発行できない。")
    void rejectsUnknownToken() {
        // トークンが見つからない状態を用意
        when(refreshTokenRepository.findByHashForUpdate(hashOf(CURRENT_TOKEN))).thenReturn(Optional.empty());

        // 再発行
        RefreshResult result = refreshService.refresh(new RefreshCommand(CURRENT_TOKEN));

        // 再発行できないの結果を返しアクセストークンを発行しないことを確認
        assertThat(result).isEqualTo(new RefreshResult.Rejected());
        verifyNoInteractions(accessTokenIssuer, refreshTokenGenerator);
    }

    /**
     * 有効期限切れの場合に再発行できないことを確認
     */
    @Test
    @DisplayName("有効期限切れなら再発行できない。")
    void rejectsExpiredToken() {
        // 有効期限が現在の時刻より前のトークンを用意
        RefreshTokenId id = RefreshTokenId.newId();
        givenStoredToken(new RefreshToken(id, USER_ID, hashOf(CURRENT_TOKEN), id, NOW.minusSeconds(1), null));

        // 再発行
        RefreshResult result = refreshService.refresh(new RefreshCommand(CURRENT_TOKEN));

        // 再発行できないの結果を返し書き込まないことを確認
        assertThat(result).isEqualTo(new RefreshResult.Rejected());
        verify(refreshTokenRepository, never()).save(any(), any(), any());
        verify(refreshTokenRepository, never()).revokeFamily(any(), any(), any());
    }

    /**
     * 無効化済みのトークンが使われた場合に同じログインのトークンをすべて無効化することを確認
     */
    @Test
    @DisplayName("無効化済みのトークンが使われたら同じログインのトークンをすべて無効化する。")
    void revokesFamilyWhenReused() {
        // 無効化済みのトークンを用意
        RefreshTokenId id = RefreshTokenId.newId();
        RefreshTokenId familyId = RefreshTokenId.newId();
        givenStoredToken(new RefreshToken(id, USER_ID, hashOf(CURRENT_TOKEN), familyId, EXPIRES_AT, ISSUED_AT));

        // 再発行
        RefreshResult result = refreshService.refresh(new RefreshCommand(CURRENT_TOKEN));

        // 再発行できないの結果を返しトークンファミリーを無効化したことを確認
        assertThat(result).isEqualTo(new RefreshResult.Rejected());
        verify(refreshTokenRepository).revokeFamily(familyId, USER_ID, NOW);
        verifyNoInteractions(accessTokenIssuer, refreshTokenGenerator);
    }

    /**
     * ハッシュ値で探すと指定したトークンが見つかる状態にする
     *
     * @param token 見つかるリフレッシュトークン
     */
    private void givenStoredToken(RefreshToken token) {
        when(refreshTokenRepository.findByHashForUpdate(hashOf(CURRENT_TOKEN))).thenReturn(Optional.of(token));
    }

    /**
     * 生のリフレッシュトークンのハッシュ値を計算
     *
     * @param rawRefreshToken 生のリフレッシュトークン
     * @return ハッシュ値
     */
    private static RefreshTokenHash hashOf(String rawRefreshToken) {
        return RefreshTokenHash.of(new RawRefreshToken(rawRefreshToken));
    }
}
