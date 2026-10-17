package io.github.kawashima_study.hoikulink.auth.domain;

import java.util.Objects;

/**
 * パスワードの値
 *
 * @param value パスワード
 */
public record RawPassword(String value) {

    /**
     * パスワードを作成
     *
     * @param value パスワード
     * @throws NullPointerException 値がnullの場合
     */
    public RawPassword {
        Objects.requireNonNull(value, "パスワードがnullです。");
    }

    /**
     * 中身を隠した文字列を返す
     * <p>パスワードをログに出力しないため
     *
     * @return 中身を隠した文字列
     */
    @Override
    public String toString() {
        return "RawPassword[***]";
    }
}
