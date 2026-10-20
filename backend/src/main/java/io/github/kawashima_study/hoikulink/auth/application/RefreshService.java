package io.github.kawashima_study.hoikulink.auth.application;

import io.github.kawashima_study.hoikulink.auth.domain.AccessToken;
import io.github.kawashima_study.hoikulink.auth.domain.AccessTokenIssuer;
import io.github.kawashima_study.hoikulink.auth.domain.RawRefreshToken;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshToken;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshTokenGenerator;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshTokenHash;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * アクセストークンの再発行のユースケースの実装
 * <p>再発行のたびにリフレッシュトークンを交換し無効化済みのトークンが使われたら同じログインのトークンをすべて無効化する
 */
@Service
class RefreshService implements RefreshUseCase {

    /** ログの出力先 */
    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshService.class);

    /** リフレッシュトークンの保存と読み出し */
    private final RefreshTokenRepository refreshTokenRepository;

    /** 生のリフレッシュトークンの作成 */
    private final RefreshTokenGenerator refreshTokenGenerator;

    /** アクセストークンの発行 */
    private final AccessTokenIssuer accessTokenIssuer;

    /** 現在の時刻 */
    private final Clock clock;

    /**
     * 再発行のユースケースを作成
     *
     * @param refreshTokenRepository リフレッシュトークンの保存と読み出し
     * @param refreshTokenGenerator 生のリフレッシュトークンの作成
     * @param accessTokenIssuer アクセストークンの発行
     * @param clock 現在の時刻
     */
    RefreshService(
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenGenerator refreshTokenGenerator,
            AccessTokenIssuer accessTokenIssuer,
            Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.accessTokenIssuer = accessTokenIssuer;
        this.clock = clock;
    }

    /**
     * リフレッシュトークンを交換しアクセストークンを再発行する
     *
     * @param command 再発行の入力
     * @return 再発行の結果（成功・再発行できないのいずれか）
     */
    @Override
    @Transactional
    public RefreshResult refresh(RefreshCommand command) {
        Instant now = Instant.now(clock);
        RefreshTokenHash tokenHash = RefreshTokenHash.of(new RawRefreshToken(command.refreshToken()));
        Optional<RefreshToken> found = refreshTokenRepository.findByHashForUpdate(tokenHash);
        if (found.isEmpty()) {
            LOGGER.info("再発行に失敗しました。リフレッシュトークンが見つかりません。");
            return new RefreshResult.Rejected();
        }

        RefreshToken current = found.get();
        if (current.isRevoked()) {
            return rejectReused(current, now);
        }
        if (!current.isUsable(now)) {
            LOGGER.info("再発行に失敗しました。リフレッシュトークンの有効期限が切れています。userId={}", current.userId());
            return new RefreshResult.Rejected();
        }
        return rotate(current, now);
    }

    /**
     * 無効化済みのトークンが使われた場合に同じログインのトークンをすべて無効化する
     * <p>盗まれたトークンが使われた可能性があるため本人にももう一度ログインしてもらう
     *
     * @param reused 使い回されたリフレッシュトークン
     * @param now 現在の時刻
     * @return 再発行できないの結果
     */
    private RefreshResult rejectReused(RefreshToken reused, Instant now) {
        refreshTokenRepository.revokeFamily(reused.familyId(), reused.userId(), now);
        LOGGER.warn(
                "無効化済みのリフレッシュトークンが使われたため同じログインのトークンをすべて無効化しました。userId={} familyId={}",
                reused.userId(),
                reused.familyId());
        return new RefreshResult.Rejected();
    }

    /**
     * リフレッシュトークンを交換しアクセストークンを発行する
     *
     * @param current 使えるリフレッシュトークン
     * @param now 現在の時刻
     * @return 再発行の成功の結果
     */
    private RefreshResult rotate(RefreshToken current, Instant now) {
        RawRefreshToken newRawRefreshToken = refreshTokenGenerator.generate();
        RefreshToken next = current.rotate(RefreshTokenHash.of(newRawRefreshToken), now);
        refreshTokenRepository.update(current, current.userId(), now); // 元のトークンの無効化を書き込む
        refreshTokenRepository.save(next, current.userId(), now);
        AccessToken accessToken = accessTokenIssuer.issue(current.userId(), now);
        LOGGER.info("アクセストークンを再発行しました。userId={}", current.userId());
        return new RefreshResult.Success(accessToken, newRawRefreshToken, next.expiresAt());
    }
}
