package io.github.kawashima_study.hoikulink.auth.domain;

import io.github.kawashima_study.hoikulink.shared.ErrorCode;

/**
 * 認証モジュールのエラーコード。
 */
public enum AuthErrorCode implements ErrorCode {
    INVALID_EMAIL_ADDRESS("メールアドレスの形式が正しくありません");

    private final String title;

    AuthErrorCode(String title) {
        this.title = title;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public String title() {
        return title;
    }
}
