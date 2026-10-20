package io.github.kawashima_study.hoikulink.auth.application;

import io.github.kawashima_study.hoikulink.auth.domain.AccessToken;
import io.github.kawashima_study.hoikulink.auth.domain.RawRefreshToken;
import java.time.Instant;
import java.util.Objects;

/**
 * アクセストークンの再発行のユースケースの結果
 * <p>使い回しを検知したときの無効化を確定させるため例外ではなく結果を返す
 */
public sealed interface RefreshResult {

    /**
     * 再発行の成功
     *
     * @param accessToken 発行したアクセストークン
     * @param refreshToken 交換した新しい生のリフレッシュトークン（Cookieに入れる）
     * @param refreshTokenExpiresAt リフレッシュトークンの有効期限（交換しても延長しない）
     */
    record Success(AccessToken accessToken, RawRefreshToken refreshToken, Instant refreshTokenExpiresAt)
            implements RefreshResult {

        /**
         * 再発行の成功の結果を作成
         *
         * @param accessToken 発行したアクセストークン
         * @param refreshToken 交換した新しい生のリフレッシュトークン
         * @param refreshTokenExpiresAt リフレッシュトークンの有効期限
         * @throws NullPointerException いずれかの値がnullの場合
         */
        public Success {
            Objects.requireNonNull(accessToken, "アクセストークンがnullです。");
            Objects.requireNonNull(refreshToken, "リフレッシュトークンがnullです。");
            Objects.requireNonNull(refreshTokenExpiresAt, "リフレッシュトークンの有効期限がnullです。");
        }
    }

    /**
     * 再発行できない（トークンがない・有効期限切れ・無効化済み）
     * <p>理由を画面に伝えずもう一度ログインしてもらう
     */
    record Rejected() implements RefreshResult {}
}
