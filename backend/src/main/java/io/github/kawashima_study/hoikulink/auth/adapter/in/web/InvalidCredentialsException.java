package io.github.kawashima_study.hoikulink.auth.adapter.in.web;

import io.github.kawashima_study.hoikulink.auth.domain.AuthErrorCode;
import io.github.kawashima_study.hoikulink.shared.ApplicationException;

/**
 * ログインの失敗（アカウントがないまたはパスワードが正しくない）
 * <p>ユースケースが結果を返した後（トランザクションの確定後）にAPIの側で投げ共通のエラー応答にする
 */
class InvalidCredentialsException extends ApplicationException {

    /**
     * 例外を作成
     */
    InvalidCredentialsException() {
        super(AuthErrorCode.INVALID_CREDENTIALS, "入力内容を確認してもう一度お試しください。");
    }
}
