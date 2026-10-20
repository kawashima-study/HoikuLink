package io.github.kawashima_study.hoikulink.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * パスワード（RawPassword）のテスト
 */
@DisplayName("パスワード")
class RawPasswordTests {

    /** テストで使用するパスワード */
    private static final String PASSWORD = "dummypassword";

    /**
     * パスワードをそのまま保持することを確認
     */
    @Test
    @DisplayName("パスワードをそのまま保持する。")
    void keepsValue() {
        // パスワードを渡して作成
        RawPassword password = new RawPassword(PASSWORD);

        // 取り出した値が渡した値と同じであることを確認
        assertThat(password.value()).isEqualTo(PASSWORD);
    }

    /**
     * null の場合に例外になることを確認
     */
    @Test
    @DisplayName("nullは受け付けない。")
    void rejectsNull() {
        // nullを渡すとNullPointerExceptionになることを確認
        assertThatThrownBy(() -> new RawPassword(null)).isInstanceOf(NullPointerException.class);
    }

    /**
     * 空文字列や空白のみの値も受け付けることを確認
     * <p>ログインではパスワードの中身を確認しないため
     *
     * @param value 空文字列または空白のみの値
     */
    @ParameterizedTest // 同じテストを値を変えて何回も動かす
    @EmptySource // 1回目は空文字列（""）を渡す
    @ValueSource(strings = {"   ", "\u3000", "\t"}) // 続けて半角の空白 全角の空白 タブを1つずつ渡す
    @DisplayName("空文字列や空白のみの値も受け付ける。")
    void acceptsBlank(String value) {
        // 空文字列 半角の空白 全角の空白 タブを渡しても例外にならないことを確認
        assertThatCode(() -> new RawPassword(value)).doesNotThrowAnyException(); // assertThatCodeは中の処理が例外を投げないことを確認する
    }

    /**
     * toString の結果にパスワードが含まれないことを確認
     */
    @Test
    @DisplayName("toString() の結果にパスワードを含めない。")
    void doesNotRevealValueInToString() {
        // パスワードを渡して作成
        RawPassword password = new RawPassword(PASSWORD);

        // パスワードの一部が含まれていないことを確認
        assertThat(password.toString()).doesNotContain(PASSWORD);
    }
}
