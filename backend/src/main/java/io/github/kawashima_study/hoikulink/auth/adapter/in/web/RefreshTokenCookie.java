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
        return builder(refreshToken.value()).maxAge(maxAge).build();
    }

    /**
     * ブラウザにCookieを削除させるCookieを作成
     * <p>同じ名前とパスで有効期間0のCookieを返すとブラウザが削除する
     *
     * @return Set-Cookieの見出しに入れるCookie
     */
    static ResponseCookie cleared() {
        return builder("").maxAge(Duration.ZERO).build();
    }

    /**
     * 共通の属性を付けたCookieの作り方を返す
     *
     * @param value Cookieの値
     * @return 共通の属性を付けたCookieの作り方
     */
    private static ResponseCookie.ResponseCookieBuilder builder(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true) // JavaScriptから読めないようにする
                .secure(true) // HTTPSのときだけ送る（localhostはブラウザが例外として扱う）
                .sameSite("Strict") // 他のサイトからのリクエストでは送らない
                .path(PATH);
    }
}
