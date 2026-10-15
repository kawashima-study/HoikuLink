package io.github.kawashima_study.hoikulink.auth.domain;

import io.github.kawashima_study.hoikulink.shared.ApplicationException;

public class InvalidEmailAddressException extends ApplicationException {

    public InvalidEmailAddressException() {
        super(AuthErrorCode.INVALID_EMAIL_ADDRESS, "254文字以内の、正しい形式のメールアドレスを入力してください。");
    }
}