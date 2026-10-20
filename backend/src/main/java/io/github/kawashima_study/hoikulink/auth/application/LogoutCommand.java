package io.github.kawashima_study.hoikulink.auth.application;

import java.util.Objects;

/**
 * ログアウトのユースケースへの入力
 *
 * @param refreshToken Cookieに入っていた生のリフレッシュトークン
 */
public record LogoutCommand(String refreshToken) {

    /**
     * ログアウトのユースケースへの入力を作成
     *
     * @param refreshToken Cookieに入っていた生のリフレッシュトークン
     * @throws NullPointerException リフレッシュトークンがnullの場合
     */
    public LogoutCommand {
        Objects.requireNonNull(refreshToken, "リフレッシュトークンがnullです。");
    }

    /**
     * 中身を隠した文字列を返す
     * <p>リフレッシュトークンをログに出力しないため
     *
     * @return 中身を隠した文字列
     */
    @Override
    public String toString() {
        return "LogoutCommand[***]";
    }
}
