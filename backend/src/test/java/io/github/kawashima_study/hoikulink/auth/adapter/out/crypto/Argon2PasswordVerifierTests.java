package io.github.kawashima_study.hoikulink.auth.adapter.out.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.kawashima_study.hoikulink.auth.domain.PasswordHash;
import io.github.kawashima_study.hoikulink.auth.domain.RawPassword;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

/**
 * パスワード照合のArgon2idでの実装（Argon2PasswordVerifier）のテスト
 * <p>DBもSpringも使わずにクラスを直接作って確認する
 */
@DisplayName("パスワードの照合（Argon2id）")
class Argon2PasswordVerifierTests {

    /** 正しいパスワード（テストデータ） */
    private static final String CORRECT_PASSWORD = "hoikulink-correct-password";

    /** テスト対象 */
    private final Argon2PasswordVerifier verifier = new Argon2PasswordVerifier(); // Springを起動せずにnewで作る

    /** テストのデータのハッシュ値を作るための部品 */
    private final Argon2PasswordEncoder encoder =
            Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8(); // ハッシュ値の文字列にパラメータが入るため照合はパラメータが違っても行える

    /**
     * 正しいパスワードの場合に一致すると判定することを確認
     */
    @Test
    @DisplayName("正しいパスワードは一致すると判定する。")
    void matchesCorrectPassword() {
        // 正しいパスワードのハッシュ値を作成
        PasswordHash passwordHash =
                new PasswordHash(encoder.encode(CORRECT_PASSWORD)); // encodeでハッシュ値の文字列を作る（毎回ソルトが変わるため同じパスワードでも結果は毎回違う）

        // 同じパスワードで照合すると一致することを確認
        assertThat(verifier.matches(new RawPassword(CORRECT_PASSWORD), passwordHash))
                .isTrue();
    }

    /**
     * 違うパスワードの場合に一致しないと判定することを確認
     */
    @Test
    @DisplayName("違うパスワードの場合に一致しないと判定する。")
    void rejectsWrongPassword() {
        // 違うパスワードのハッシュ値を作成
        PasswordHash passwordHashDifferent = new PasswordHash(encoder.encode("different-password"));

        // 違うパスワードで照合すると一致しないことを確認
        assertThat(verifier.matches(new RawPassword(CORRECT_PASSWORD), passwordHashDifferent))
                .isFalse();
    }

    /**
     * ダミーの照合は常に一致しないと判定することを確認
     */
    @Test
    @DisplayName("ダミーの照合は常に一致しないと判定する。")
    void matchesDummyReturnsFalse() {
        // どんなパスワードを渡してもダミーの照合はfalseになることを確認
        assertThat(verifier.matchesDummy(new RawPassword(CORRECT_PASSWORD)))
                .isFalse(); // ハッシュ値は渡さない（ダミーのハッシュ値はArgon2PasswordVerifierの中にある）
    }
}
