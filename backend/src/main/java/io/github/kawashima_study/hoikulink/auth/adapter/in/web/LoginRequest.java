package io.github.kawashima_study.hoikulink.auth.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * ログインAPIのリクエストの本文
 *
 * @param email ログインID（メールアドレス）
 * @param password パスワード
 */
record LoginRequest(
        @NotBlank(message = "メールアドレスを入力してください。") String email, // 空文字列と空白のみも入力なしとする
        @NotEmpty(message = "パスワードを入力してください。") String password) { // 空白のみのパスワードもありえるため空文字列だけを入力なしとする

    /**
     * 中身を隠した文字列を返す
     * <p>メールアドレスとパスワードをログに出力しないため
     *
     * @return 中身を隠した文字列
     */
    @Override
    public String toString() {
        return "LoginRequest[***]";
    }
}
