package io.github.kawashima_study.hoikulink.auth.application;

import java.util.Objects;

/**
 * アクセストークンの再発行のユースケースへの入力
 *
 * @param refreshToken Cookieに入っていた生のリフレッシュトークン
 */
public record RefreshCommand(String refreshToken) {

    /**
     * 再発行のユースケースへの入力を作成
     *
     * @param refreshToken Cookieに入っていた生のリフレッシュトークン
     * @throws NullPointerException リフレッシュトークンがnullの場合
     */
    public RefreshCommand {
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
        return "RefreshCommand[***]";
    }
}
