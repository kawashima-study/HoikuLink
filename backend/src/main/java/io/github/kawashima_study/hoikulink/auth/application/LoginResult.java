package io.github.kawashima_study.hoikulink.auth.application;

import io.github.kawashima_study.hoikulink.auth.domain.AccessToken;
import io.github.kawashima_study.hoikulink.auth.domain.RawRefreshToken;
import java.time.Instant;
import java.util.Objects;

/**
 * ログインのユースケースの結果
 * <p>例外を投げずに結果を返すことで失敗回数の書き込みがトランザクションの取り消しで消えないようにする
 * <p>sealedにすると結果の種類を増やしたときにAPIの側の扱い漏れがコンパイルエラーで分かる
 */
public sealed interface LoginResult { // permitsを書かない場合は同じファイルの中のクラスだけが実装できる

    /**
     * ログインの成功
     *
     * @param accessToken 発行したアクセストークン
     * @param refreshToken 発行した生のリフレッシュトークン（Cookieに入れる）
     * @param refreshTokenExpiresAt リフレッシュトークンの有効期限（Cookieの有効期限に使う）
     */
    record Success(AccessToken accessToken, RawRefreshToken refreshToken, Instant refreshTokenExpiresAt)
            implements LoginResult {

        /**
         * ログインの成功の結果を作成
         *
         * @param accessToken 発行したアクセストークン
         * @param refreshToken 発行した生のリフレッシュトークン
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
     * ログインの失敗（アカウントがないまたはパスワードが正しくない）
     * <p>アカウントの有無を区別しないため1つの種類にまとめる
     */
    record InvalidCredentials() implements LoginResult {}

    /**
     * ロック中のアカウントでのログイン（パスワードが正しい場合だけ）
     */
    record AccountLocked() implements LoginResult {}
}
