package io.github.kawashima_study.hoikulink.auth.domain;

import java.util.Locale;
import java.util.Objects;

/**
 * ログインID（メールアドレス）
 *
 * @param value 前後の空白を取り除き小文字にそろえたメールアドレス
 */
public record EmailAddress(String value) {

    /** メールアドレスの最大の文字数 */
    private static final int MAX_LENGTH = 254;

    /**
     * メールアドレスを作成
     * <p>前後の空白を取り除き小文字にそろえてから長さと形式を確認する
     *
     * @param value 入力されたメールアドレス
     * @throws NullPointerException 値がnullの場合
     * @throws InvalidEmailAddressException 値が空、254文字の超過、形式に誤りがある場合
     */
    public EmailAddress {
        Objects.requireNonNull(value, "メールアドレスがnullです。");
        // 全角の空白やタブも取り除く
        value = value.strip();
        // 利用者の環境の言語に左右されないようにロケールを固定して小文字化
        value = value.toLowerCase(Locale.ROOT);
        // 文字数の確認
        if (value.isEmpty() || value.length() > MAX_LENGTH) {
            throw new InvalidEmailAddressException();
        }
        // 形式の確認（空白を含まず「@」が1つ、@の後ろに「.」を含む）
        if (!value.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new InvalidEmailAddressException();
        }
    }

    /**
     * 中身を隠した文字列を返す
     * <p>個人の情報をログに出力しないため
     *
     * @return 中身を隠した文字列
     */
    @Override
    public String toString() {
        return "EmailAddress[***]";
    }
}
