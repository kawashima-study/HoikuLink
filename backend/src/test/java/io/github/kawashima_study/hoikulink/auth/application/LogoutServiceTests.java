package io.github.kawashima_study.hoikulink.auth.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.kawashima_study.hoikulink.auth.domain.RawRefreshToken;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshToken;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshTokenHash;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshTokenRepository;
import io.github.kawashima_study.hoikulink.shared.BusinessTimeZone;
import io.github.kawashima_study.hoikulink.shared.UserId;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ログアウトのユースケース（LogoutService）のテスト
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ログアウトのユースケース")
class LogoutServiceTests {

    /** 現在の時刻（テストデータ） */
    private static final Instant NOW = Instant.parse("2026-10-20T03:00:00Z");

    /** Cookieに入っていた生のリフレッシュトークン（テストデータ） */
    private static final String REFRESH_TOKEN = "test-refresh-token";

    /** リフレッシュトークンの保存と読み出しの仮の部品 */
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    /** テスト対象 */
    private LogoutService logoutService;

    /**
     * 各テストの前にテスト対象を作成
     */
    @BeforeEach
    void setUp() {
        logoutService = new LogoutService(refreshTokenRepository, Clock.fixed(NOW, BusinessTimeZone.JAPAN));
    }

    /**
     * トークンが見つかった場合に同じログインのトークンをすべて無効化することを確認
     */
    @Test
    @DisplayName("トークンが見つかれば同じログインのトークンをすべて無効化する。")
    void revokesFamily() {
        // 保存されているトークンを用意
        UserId userId = UserId.newId();
        RefreshToken token = RefreshToken.issue(userId, hashOf(REFRESH_TOKEN), NOW);
        when(refreshTokenRepository.findByHashForUpdate(hashOf(REFRESH_TOKEN))).thenReturn(Optional.of(token));

        // ログアウト
        logoutService.logout(new LogoutCommand(REFRESH_TOKEN));

        // トークンファミリーをアカウント自身を更新者として無効化したことを確認
        verify(refreshTokenRepository).revokeFamily(token.familyId(), userId, NOW);
    }

    /**
     * トークンが見つからない場合に何もしないことを確認
     */
    @Test
    @DisplayName("トークンが見つからなければ何もしない。")
    void doesNothingForUnknownToken() {
        // トークンが見つからない状態を用意
        when(refreshTokenRepository.findByHashForUpdate(hashOf(REFRESH_TOKEN))).thenReturn(Optional.empty());

        // ログアウト
        logoutService.logout(new LogoutCommand(REFRESH_TOKEN));

        // 無効化しないことを確認
        verify(refreshTokenRepository, never()).revokeFamily(any(), any(), any());
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
