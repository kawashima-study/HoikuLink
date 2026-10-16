package io.github.kawashima_study.hoikulink.auth.domain;

import java.util.Objects;

/**
 * パスワードのハッシュ値
 * <p>ハッシュの計算は行わず計算済みの値を保持する
 *
 * @param value 計算済みのハッシュ値
 */
public record PasswordHash(String value) {

    /**
     * パスワードハッシュを作成
     * <p>計算済みの値のため空白の除去などの加工はしない
     *
     * @param value 計算済みのハッシュ値
     * @throws NullPointerException 値が null の場合
     * @throws IllegalArgumentException 空文字列または空白のみの場合
     */
    public PasswordHash {
        Objects.requireNonNull(value, "パスワードハッシュがnullです。");
        if (value.isBlank()) {
            throw new IllegalArgumentException("パスワードハッシュが空です。");
        }
    }

    /**
     * 中身を隠した文字列を返す
     * <p>ハッシュ値をログに出力しないため
     *
     * @return 中身を隠した文字列
     */
    @Override
    public String toString() {
        return "PasswordHash[***]";
    }
}
