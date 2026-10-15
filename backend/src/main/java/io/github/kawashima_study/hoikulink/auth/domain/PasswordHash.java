package io.github.kawashima_study.hoikulink.auth.domain;

import java.util.Objects;

/**
 * パスワードのハッシュ値。ハッシュの計算は行わず、計算済みの値を保持する。
 */
public record PasswordHash(String value) {

    public PasswordHash {
        Objects.requireNonNull(value, "パスワードハッシュがnullです。");
        if (value.isBlank()) {
            throw new IllegalArgumentException("パスワードハッシュが空です。");
        }
    }

    @Override
    public String toString() {
        return "PasswordHash[***]";
    }
}