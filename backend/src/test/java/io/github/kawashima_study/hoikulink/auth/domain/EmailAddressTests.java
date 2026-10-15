package io.github.kawashima_study.hoikulink.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("メールアドレス（ログインID）")
class EmailAddressTests {

    @Test
    @DisplayName("nullは受け付けない")
    void rejectsNull() {
        assertThatThrownBy(() -> new EmailAddress(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("前後の空白を取り除き、小文字にそろえる")
    void normalizesToLowerCase() {
        assertThat(new EmailAddress("  Hanako@Example.COM ").value()).isEqualTo("hanako@example.com");
    }

    @Test
    @DisplayName("前後の全角空白やタブも取り除き、小文字にそろえる")
    void stripsFullWidthSpaceAndTab() {
        // 前に全角空白とタブ、後ろに全角空白が付いた文字列を渡す
        EmailAddress email = new EmailAddress("\u3000\tHanako@Example.COM\u3000");
        // 空白が取れて、小文字になっているか確認
        assertThat(email.value()).isEqualTo("hanako@example.com");
    }

    @Test
    @DisplayName("大文字・小文字だけが違うメールアドレスは、同じとみなす")
    void treatsCaseInsensitiveAddressesAsEqual() {
        assertThat(new EmailAddress("Hanako@Example.com")).isEqualTo(new EmailAddress("hanako@example.com"));
    }

    @Test
    @DisplayName("254文字のアドレスの前後に空白を付けたアドレスを受け付ける")
    void countsLengthAfterStripping() {
        String maxLength = "a".repeat(242) + "@example.com";
        // 254文字のアドレスの前後に空白を付けた文字列を渡す
        EmailAddress email = new EmailAddress("\u3000\t" + maxLength + "\u3000");
        // 小文字かつ空白のない254文字のアドレスか確認
        assertThat(email.value()).isEqualTo(maxLength);
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {"   ", "hanako", "hanako@", "@example.com", "hana ko@example.com", "hanako@example", "a@b@example.com", "a@.com", "a@example."})
    @DisplayName("メールアドレスの形ではない値は受け付けない")
    void rejectsInvalidAddress(String value) {
        assertThatThrownBy(() -> new EmailAddress(value)).isInstanceOf(InvalidEmailAddressException.class);
    }

    @Test
    @DisplayName("254文字ちょうどのメールアドレスは受け付ける")
    void accepts254Characters() {
        String maxLength = "a".repeat(242) + "@example.com";
        // 254文字ちょうどの文字列を渡す
        EmailAddress email = new EmailAddress(maxLength);
        // 小文字かつ空白のないアドレスか確認
        assertThat(email.value()).isEqualTo(maxLength);
    }

    @Test
    @DisplayName("254文字を超えるメールアドレスは受け付けない")
    void rejectsTooLongAddress() {
        String tooLong = "a".repeat(243) + "@example.com";

        assertThatThrownBy(() -> new EmailAddress(tooLong)).isInstanceOf(InvalidEmailAddressException.class);
    }

    @Test
    @DisplayName("toString() の結果に、メールアドレスの中身を含めない")
    void doesNotRevealValueInToString() {
        // アドレスを渡す
        EmailAddress email = new EmailAddress("hanako@Example.com");
        // アドレスが表示されず、マスキングされていることを確認
        assertThat(email.toString()).doesNotContain("hanako");
        assertThat(email.toString()).doesNotContain("example");
    }
}
