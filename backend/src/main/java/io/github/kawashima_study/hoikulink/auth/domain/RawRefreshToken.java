package io.github.kawashima_study.hoikulink.auth.domain;

import java.util.Objects;

/**
 * 生のリフレッシュトークン（Cookieに入れる文字列）
 * <p>DBにはこの値ではなくハッシュ値（RefreshTokenHash）を保存する
 *
 * @param value 生のリフレッシュトークン
 */
public record RawRefreshToken(String value) {

    /**
     * 生のリフレッシュトークンを作成
     *
     * @param value 生のリフレッシュトークン
     * @throws NullPointerException 値がnullの場合
     */
    public RawRefreshToken {
        Objects.requireNonNull(value, "リフレッシュトークンがnullです。");
    }

    /**
     * 中身を隠した文字列を返す
     * <p>リフレッシュトークンをログに出力しないため
     *
     * @return 中身を隠した文字列
     */
    @Override
    public String toString() {
        return "RawRefreshToken[***]";
    }
}
