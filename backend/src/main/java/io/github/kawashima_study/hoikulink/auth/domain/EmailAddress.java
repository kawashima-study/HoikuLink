package io.github.kawashima_study.hoikulink.auth.domain;

import java.util.Locale;
import java.util.Objects;

/** ログインID（メールアドレス）。 */
public record EmailAddress(String value) {

    private static final int MAX_LENGTH = 254;

    public EmailAddress {
        Objects.requireNonNull(value, "メールアドレスがnullです。");
        // 空白削除
        value = value.strip();
        // 小文字化
        value = value.toLowerCase(Locale.ROOT);
        // 文字数チェック
        if (value.isEmpty() || value.length() > MAX_LENGTH) {
            throw new InvalidEmailAddressException();
        }
        // メールアドレスの形式チェック
        if (!value.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new InvalidEmailAddressException();
        }
    }

    @Override
    public String toString() {
        return "EmailAddress[***]";
    }
}
