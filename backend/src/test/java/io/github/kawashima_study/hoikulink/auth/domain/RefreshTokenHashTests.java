package io.github.kawashima_study.hoikulink.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * リフレッシュトークンのハッシュ値（RefreshTokenHash）のテスト
 */
@DisplayName("リフレッシュトークンのハッシュ値")
class RefreshTokenHashTests {

    /** 生のリフレッシュトークン（テストデータ） */
    private static final String REFRESH_TOKEN = "abc";

    /** "abc"のSHA-256のハッシュ値（公式の確認用の値） */
    private static final String HASH = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

    /** 大文字を含むハッシュ値 */
    private static final String UPPERCASE_HASH = "BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD";

    /** 63文字のハッシュ値 */
    private static final String HASH_LENGTH_63 = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015a";

    /** 65文字のハッシュ値 */
    private static final String HASH_LENGTH_65 = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad0";

    /** 16進数でない文字を含むハッシュ値 */
    private static final String INVALID_HEX_HASH = "ga7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

    /**
     * 生のリフレッシュトークンから正しいハッシュ値を計算することを確認
     */
    @Test
    @DisplayName("生のリフレッシュトークンから正しいハッシュ値を計算する。")
    void generateExpectedHash() {
        // 公式の確認用の値"abc"からハッシュ値を計算
        RefreshTokenHash hash = RefreshTokenHash.of(new RawRefreshToken(REFRESH_TOKEN));

        // 計算した値が公式の確認用のハッシュ値と同じであることを確認
        assertThat(hash.value()).isEqualTo(HASH);
    }

    /**
     * 同じトークンからは同じハッシュ値になることを確認
     * <p>再発行のときにCookieの値から計算したハッシュ値でDBを検索するため
     */
    @Test
    @DisplayName("同じトークンからは同じハッシュ値になる。")
    void verifyDeterministicHashing() {
        // 同じトークンからハッシュ値を2回計算
        RefreshTokenHash first = RefreshTokenHash.of(new RawRefreshToken(REFRESH_TOKEN));
        RefreshTokenHash second = RefreshTokenHash.of(new RawRefreshToken(REFRESH_TOKEN));

        // 2つのハッシュ値が同じであることを確認（recordのため中身で比べる）
        assertThat(first).isEqualTo(second);
    }

    /**
     * 正しい形式のハッシュ値をそのまま保持することを確認
     * <p>DBから読み込んだハッシュ値で作る場合を想定
     */
    @Test
    @DisplayName("正しい形式のハッシュ値をそのまま保持する。")
    void keepsValidHash() {
        // 正しい形式のハッシュ値を渡して作成
        RefreshTokenHash hash = new RefreshTokenHash(HASH);

        // 取り出した値が渡した値と同じであることを確認
        assertThat(hash.value()).isEqualTo(HASH);
    }

    /**
     * ハッシュ値がnullの場合に例外になることを確認
     */
    @Test
    @DisplayName("ハッシュ値のnullは受け付けない。")
    void rejectsNull() {
        // nullを渡すとNullPointerExceptionになることを確認
        assertThatThrownBy(() -> new RefreshTokenHash(null)).isInstanceOf(NullPointerException.class);
    }

    /**
     * 生のリフレッシュトークンがnullの場合に例外になることを確認
     */
    @Test
    @DisplayName("生のリフレッシュトークンのnullからは計算しない。")
    void rejectsNullRawRefreshToken() {
        // nullを渡すとNullPointerExceptionになることを確認
        assertThatThrownBy(() -> RefreshTokenHash.of(null)).isInstanceOf(NullPointerException.class);
    }

    /**
     * 形式の誤った値の場合に例外になることを確認
     *
     * @param value 形式の誤ったハッシュ値
     */
    @ParameterizedTest // 同じテストを値を変えて何回も動かす
    @EmptySource // 1回目は空文字列（""）を渡す
    @ValueSource(strings = {UPPERCASE_HASH, HASH_LENGTH_63, HASH_LENGTH_65, INVALID_HEX_HASH}) // 続けて誤りの種類ごとの値を1つずつ渡す
    @DisplayName("形式の誤った値は受け付けない。")
    void rejectsInvalidFormat(String value) {
        // 形式の誤った値を渡すとIllegalArgumentExceptionになることを確認
        assertThatThrownBy(() -> new RefreshTokenHash(value)).isInstanceOf(IllegalArgumentException.class);
    }
}
