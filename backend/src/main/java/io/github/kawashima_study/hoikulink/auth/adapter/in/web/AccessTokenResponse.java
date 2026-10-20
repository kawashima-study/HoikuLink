package io.github.kawashima_study.hoikulink.auth.adapter.in.web;

import io.github.kawashima_study.hoikulink.auth.domain.AccessToken;
import io.github.kawashima_study.hoikulink.auth.domain.RawRefreshToken;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

/**
 * ログインと再発行のAPIの成功の応答の本文
 *
 * @param accessToken アクセストークン（JWT）
 * @param tokenType トークンの種類（Bearer）
 * @param expiresIn アクセストークンの有効期限までの秒数
 */
record AccessTokenResponse(String accessToken, String tokenType, long expiresIn) {

    /** トークンの種類 */
    private static final String TOKEN_TYPE = "Bearer";

    /**
     * 本文にアクセストークンを入れCookieにリフレッシュトークンを入れた200の応答を作成
     * <p>ログインと再発行で同じ形の応答を返すため1か所にまとめる
     *
     * @param accessToken アクセストークン
     * @param refreshToken 生のリフレッシュトークン
     * @param cookieMaxAge リフレッシュトークンのCookieの有効期間
     * @return 200の応答
     */
    static ResponseEntity<AccessTokenResponse> ok(
            AccessToken accessToken, RawRefreshToken refreshToken, Duration cookieMaxAge) {
        AccessTokenResponse body = new AccessTokenResponse(
                accessToken.value(), TOKEN_TYPE, accessToken.expiresIn().toSeconds());
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        RefreshTokenCookie.of(refreshToken, cookieMaxAge).toString())
                .cacheControl(CacheControl.noStore()) // トークンを含む応答をブラウザや途中の機器に保存させない
                .body(body);
    }

    /**
     * 中身を隠した文字列を返す
     * <p>アクセストークンをログに出力しないため
     *
     * @return 中身を隠した文字列
     */
    @Override
    public String toString() {
        return "AccessTokenResponse[***]";
    }
}
