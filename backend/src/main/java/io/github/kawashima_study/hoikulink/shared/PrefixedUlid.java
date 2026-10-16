package io.github.kawashima_study.hoikulink.shared;

import com.github.f4b6a3.ulid.UlidCreator;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 「大文字3文字のプレフィックス＋ULID（26文字）」のIDを作成し形式を確認する
 */
public final class PrefixedUlid {

    /** IDの長さ（DBの列の長さ CHAR(29) と同じ） */
    public static final int LENGTH = 29;

    /** プレフィックスの形式（大文字3文字） */
    private static final Pattern PREFIX_PATTERN = Pattern.compile("[A-Z]{3}");

    /**
     * IDの形式
     * <p>ULIDは Crockford Base32（I L O U を除く大文字と数字）で先頭の1文字は0〜7に限られる
     */
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Z]{3}[0-7][0-9A-HJKMNP-TV-Z]{25}");

    /**
     * 道具のみを持つクラスのためインスタンスを作成させない
     */
    private PrefixedUlid() {}

    /**
     * 新しいIDを作成
     * <p>同じミリ秒に作成したIDでも推測されないようランダムな部分は毎回作り直す
     *
     * @param prefix 大文字3文字のプレフィックス（例 USR）
     * @return 作成したID
     * @throws NullPointerException プレフィックスが null の場合
     * @throws IllegalArgumentException プレフィックスが大文字3文字でない場合
     */
    public static String generate(String prefix) {
        requireValidPrefix(prefix);
        return prefix + UlidCreator.getUlid();
    }

    /**
     * IDの形式が正しく指定したプレフィックスで始まることを確認
     *
     * @param prefix 大文字3文字のプレフィックス（例 USR）
     * @param value 確認するID
     * @return 確認したID
     * @throws NullPointerException プレフィックスまたはIDが null の場合
     * @throws IllegalArgumentException プレフィックスが大文字3文字でない場合
     * @throws InvalidIdException IDの形式が正しくない場合またはプレフィックスが違う場合
     */
    public static String requireValid(String prefix, String value) {
        requireValidPrefix(prefix);
        Objects.requireNonNull(value, "IDがnullです。");
        if (!ID_PATTERN.matcher(value).matches() || !value.startsWith(prefix)) {
            // ログに不正な文字列が残るのを防ぐため入力された値をメッセージに入れない
            throw new InvalidIdException(prefix);
        }
        return value;
    }

    /**
     * プレフィックスの形式を確認
     *
     * @param prefix 確認するプレフィックス
     * @throws NullPointerException プレフィックスが null の場合
     * @throws IllegalArgumentException プレフィックスが大文字3文字でない場合
     */
    private static void requireValidPrefix(String prefix) {
        Objects.requireNonNull(prefix, "プレフィックスがnullです。");
        if (!PREFIX_PATTERN.matcher(prefix).matches()) {
            throw new IllegalArgumentException("プレフィックスは大文字3文字にしてください。");
        }
    }
}
