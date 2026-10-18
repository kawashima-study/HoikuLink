package io.github.kawashima_study.hoikulink.auth.domain;

import java.time.Duration;
import java.util.Objects;

/**
 * アクセストークン（JWT）と有効期限までの時間
 *
 * @param value アクセストークン（JWT）
 * @param expiresIn 有効期限までの時間
 */
public record AccessToken(String value, Duration expiresIn) {

    /**
     * アクセストークンを作成
     *
     * @param value アクセストークン（JWT）
     * @param expiresIn 有効期限までの時間
     * @throws NullPointerException アクセストークンまたは有効期限までの時間がnullの場合
     */
    public AccessToken {
        Objects.requireNonNull(value, "アクセストークンがnullです。");
        Objects.requireNonNull(expiresIn, "有効期限までの時間がnullです。");
    }

    /**
     * 中身を隠した文字列を返す
     * <p>アクセストークンをログに出力しないため
     *
     * @return 中身を隠した文字列
     */
    @Override
    public String toString() {
        return "AccessToken[***]";
    }
}
