package io.github.kawashima_study.hoikulink.auth.adapter.in.web;

import io.github.kawashima_study.hoikulink.auth.domain.RawRefreshToken;
import java.time.Duration;
import org.springframework.http.ResponseCookie;

/**
 * リフレッシュトークンを入れるCookieの作成
 * <p>ログイン・再発行・ログアウトのAPIで同じ属性のCookieを使うため1か所にまとめる
 */
final class RefreshTokenCookie {

    /** Cookieの名前 */
    static final String NAME = "refresh_token";

    /** Cookieを送る先のパス（ログイン・再発行・ログアウトのAPIだけに送る） */
    private static final String PATH = "/api/v1/auth";

    /**
     * インスタンスを作らせないためのコンストラクタ
     */
    private RefreshTokenCookie() {}

    /**
     * リフレッシュトークンを入れたCookieを作成
     *
     * @param refreshToken 生のリフレッシュトークン
     * @param maxAge Cookieの有効期間
     * @return Set-Cookieの見出しに入れるCookie
     */
    static ResponseCookie of(RawRefreshToken refreshToken, Duration maxAge) {
        return ResponseCookie.from(NAME, refreshToken.value())
                .httpOnly(true) // JavaScriptから読めないようにする
                .secure(true) // HTTPSのときだけ送る（localhostはブラウザが例外として扱う）
                .sameSite("Strict") // 他のサイトからのリクエストでは送らない
                .path(PATH)
                .maxAge(maxAge)
                .build();
    }
}
