package io.github.kawashima_study.hoikulink.auth.application;

import io.github.kawashima_study.hoikulink.auth.domain.RawRefreshToken;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshTokenHash;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshTokenRepository;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ログアウトのユースケースの実装
 */
@Service
class LogoutService implements LogoutUseCase {

    /** ログの出力先 */
    private static final Logger LOGGER = LoggerFactory.getLogger(LogoutService.class);

    /** リフレッシュトークンの保存と読み出し */
    private final RefreshTokenRepository refreshTokenRepository;

    /** 現在の時刻 */
    private final Clock clock;

    /**
     * ログアウトのユースケースを作成
     *
     * @param refreshTokenRepository リフレッシュトークンの保存と読み出し
     * @param clock 現在の時刻
     */
    LogoutService(RefreshTokenRepository refreshTokenRepository, Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
    }

    /**
     * 同じログインのリフレッシュトークンをすべて無効化する
     *
     * @param command ログアウトの入力
     */
    @Override
    @Transactional
    public void logout(LogoutCommand command) {
        Instant now = Instant.now(clock);
        RefreshTokenHash tokenHash = RefreshTokenHash.of(new RawRefreshToken(command.refreshToken()));
        refreshTokenRepository
                .findByHashForUpdate(tokenHash)
                .ifPresent(
                        token -> { // 見つかった場合だけ無効化する
                            refreshTokenRepository.revokeFamily(token.familyId(), token.userId(), now);
                            LOGGER.info("ログアウトしました。userId={}", token.userId());
                        });
    }
}
