package io.github.kawashima_study.hoikulink.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * メールアドレス（EmailAddress）のテスト
 */
@DisplayName("メールアドレス（ログインID）")
class EmailAddressTests {

    /** 254文字ちょうどのメールアドレス（242文字＋@example.com の12文字） */
    private static final String MAX_LENGTH_ADDRESS = "a".repeat(242) + "@example.com";

    /**
     * null の場合に例外になることを確認
     */
    @Test
    @DisplayName("nullは受け付けない。")
    void rejectsNull() {
        // null を渡すと NullPointerException になることを確認
        assertThatThrownBy(() -> new EmailAddress(null)).isInstanceOf(NullPointerException.class);
    }

    /**
     * 前後の半角の空白を取り除き小文字にそろえることを確認
     */
    @Test
    @DisplayName("前後の空白を取り除き小文字にそろえる。")
    void normalizesToLowerCase() {
        // 前後に半角の空白が付いた大文字を含むアドレスを渡す
        EmailAddress email = new EmailAddress("  Hanako@Example.COM ");

        // 空白が取れ小文字になっていることを確認
        assertThat(email.value()).isEqualTo("hanako@example.com");
    }

    /**
     * 前後の全角の空白やタブも取り除き小文字にそろえることを確認
     */
    @Test
    @DisplayName("前後の全角空白やタブも取り除き小文字にそろえる。")
    void stripsFullWidthSpaceAndTab() {
        // 前に全角の空白とタブ 後ろに全角の空白が付いたアドレスを渡す
        EmailAddress email = new EmailAddress("\u3000\tHanako@Example.COM\u3000");

        // 空白が取れ小文字になっていることを確認
        assertThat(email.value()).isEqualTo("hanako@example.com");
    }

    /**
     * 大文字と小文字のみが違うメールアドレスを同じとみなすことを確認
     */
    @Test
    @DisplayName("大文字・小文字だけが違うメールアドレスは同じとみなす。")
    void treatsCaseInsensitiveAddressesAsEqual() {
        // 大文字を含むアドレスと小文字のみのアドレスが等しいことを確認
        assertThat(new EmailAddress("Hanako@Example.com")).isEqualTo(new EmailAddress("hanako@example.com"));
    }

    /**
     * 254文字ちょうどのメールアドレスを受け付けることを確認
     */
    @Test
    @DisplayName("254文字ちょうどのメールアドレスは受け付ける。")
    void accepts254Characters() {
        // 254文字ちょうどのアドレスを渡す
        EmailAddress email = new EmailAddress(MAX_LENGTH_ADDRESS);

        // 渡した値のまま受け付けられていることを確認
        assertThat(email.value()).isEqualTo(MAX_LENGTH_ADDRESS);
    }

    /**
     * 文字数を前後の空白を取り除いた後に数えることを確認
     */
    @Test
    @DisplayName("254文字のアドレスの前後に空白を付けても受け付ける。")
    void countsLengthAfterStripping() {
        // 254文字のアドレスの前後に全角の空白とタブを付けて渡す
        EmailAddress email = new EmailAddress("\u3000\t" + MAX_LENGTH_ADDRESS + "\u3000");

        // 空白を取り除いた254文字のアドレスになっていることを確認
        assertThat(email.value()).isEqualTo(MAX_LENGTH_ADDRESS);
    }

    /**
     * 254文字を超えるメールアドレスの場合に例外になることを確認
     */
    @Test
    @DisplayName("254文字を超えるメールアドレスは受け付けない。")
    void rejectsTooLongAddress() {
        // 255文字のアドレスを作成
        String tooLong = "a".repeat(243) + "@example.com";

        // 渡すと InvalidEmailAddressException になることを確認
        assertThatThrownBy(() -> new EmailAddress(tooLong)).isInstanceOf(InvalidEmailAddressException.class);
    }

    /**
     * メールアドレスの形式でない値の場合に例外になることを確認
     *
     * @param value メールアドレスの形式でない値
     */
    @ParameterizedTest
    @EmptySource
    @ValueSource(
            strings = {
                "   ",
                "hanako",
                "hanako@",
                "@example.com",
                "hana ko@example.com",
                "hanako@example",
                "a@b@example.com",
                "a@.com",
                "a@example."
            })
    @DisplayName("メールアドレスの形ではない値は受け付けない。")
    void rejectsInvalidAddress(String value) {
        // 形式でない値を渡すと InvalidEmailAddressException になることを確認
        assertThatThrownBy(() -> new EmailAddress(value)).isInstanceOf(InvalidEmailAddressException.class);
    }

    /**
     * toString の結果にメールアドレスの中身が含まれないことを確認
     */
    @Test
    @DisplayName("toString() の結果にメールアドレスの中身を含めない。")
    void doesNotRevealValueInToString() {
        // アドレスを渡して作成
        EmailAddress email = new EmailAddress("hanako@Example.com");

        // @ の前と後ろのどちらも含まれていないことを確認
        assertThat(email.toString()).doesNotContain("hanako").doesNotContain("example");
    }
}
