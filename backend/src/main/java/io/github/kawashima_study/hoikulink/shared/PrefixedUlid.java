package io.github.kawashima_study.hoikulink.shared;

import com.github.f4b6a3.ulid.UlidCreator;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 「大文字3文字のプレフィックス＋ULID（26文字）」のIDを作り、形式を確かめる。
 */
public final class PrefixedUlid {

    /** DBの列の長さ（CHAR(29)）と同じ。 */
    public static final int LENGTH = 29;

    private static final Pattern PREFIX_PATTERN = Pattern.compile("[A-Z]{3}");

    // ULIDは Crockford Base32（I・L・O・Uを除く大文字と数字）で、先頭の1文字は0〜7に限られる。
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Z]{3}[0-7][0-9A-HJKMNP-TV-Z]{25}");

    private PrefixedUlid() {}

    /**
     * 新しいIDを作る。同じミリ秒に作ったIDでも推測されないよう、ランダムな部分は毎回作り直す。
     */
    public static String generate(String prefix) {
        requireValidPrefix(prefix);
        return prefix + UlidCreator.getUlid();
    }

    /**
     * IDの形式が正しく、指定したプレフィックスで始まることを確かめる。
     */
    public static String requireValid(String prefix, String value) {
        requireValidPrefix(prefix);
        Objects.requireNonNull(value, "IDがnullです。");
        if (!ID_PATTERN.matcher(value).matches() || !value.startsWith(prefix)) {
            // 入力された値をそのまま例外のメッセージに入れない（ログに不正な文字列が残るのを防ぐ）。
            throw new InvalidIdException(prefix);
        }
        return value;
    }

    private static void requireValidPrefix(String prefix) {
        Objects.requireNonNull(prefix, "プレフィックスがnullです。");
        if (!PREFIX_PATTERN.matcher(prefix).matches()) {
            throw new IllegalArgumentException("プレフィックスは大文字3文字にしてください。");
        }
    }
}
