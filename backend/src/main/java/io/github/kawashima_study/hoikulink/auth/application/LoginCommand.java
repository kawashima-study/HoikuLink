package io.github.kawashima_study.hoikulink.auth.application;

import java.util.Objects;

/**
 * ログインのユースケースへの入力
 * <p>APIの側はドメインの型を使わずこの型に詰め替えて渡す
 *
 * @param email 入力されたメールアドレス
 * @param password 入力されたパスワード
 */
public record LoginCommand(String email, String password) {

    /**
     * ログインのユースケースへの入力を作成
     *
     * @param email 入力されたメールアドレス
     * @param password 入力されたパスワード
     * @throws NullPointerException メールアドレスまたはパスワードがnullの場合
     */
    public LoginCommand {
        Objects.requireNonNull(email, "メールアドレスがnullです。");
        Objects.requireNonNull(password, "パスワードがnullです。");
    }

    /**
     * 中身を隠した文字列を返す
     * <p>メールアドレスとパスワードをログに出力しないため
     *
     * @return 中身を隠した文字列
     */
    @Override
    public String toString() {
        return "LoginCommand[***]";
    }
}
