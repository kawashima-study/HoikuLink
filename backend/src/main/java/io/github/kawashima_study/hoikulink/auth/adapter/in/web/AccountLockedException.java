package io.github.kawashima_study.hoikulink.auth.adapter.in.web;

import io.github.kawashima_study.hoikulink.auth.domain.AuthErrorCode;
import io.github.kawashima_study.hoikulink.shared.ApplicationException;

/**
 * ロック中のアカウントでのログイン（パスワードが正しい場合だけ）
 * <p>ユースケースが結果を返した後（トランザクションの確定後）にAPIの側で投げ共通のエラー応答にする
 */
class AccountLockedException extends ApplicationException {

    /**
     * 例外を作成
     */
    AccountLockedException() {
        super(AuthErrorCode.ACCOUNT_LOCKED, "園長または主任にロックの解除を依頼してください。");
    }
}
