package io.github.kawashima_study.hoikulink.auth.adapter.out.crypto;

import io.github.kawashima_study.hoikulink.auth.domain.PasswordHash;
import io.github.kawashima_study.hoikulink.auth.domain.PasswordVerifier;
import io.github.kawashima_study.hoikulink.auth.domain.RawPassword;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * パスワードの照合のArgon2idでの実装
 * <p>パラメータはメモリ12MiB 反復3回 並列度1とする
 */
@Component // DBではない外の仕組み（暗号の計算）を使う部品としてSpringにBeanを作らせる
class Argon2PasswordVerifier implements PasswordVerifier {

    /**
     * ソルトの長さ（バイト）
     */
    private static final int SALT_LENGTH = 16;

    /**
     * ハッシュ値の長さ（バイト）
     */
    private static final int HASH_LENGTH = 32;

    /**
     * 並列度
     */
    private static final int PARALLELISM = 1;

    /**
     * 使うメモリ（KiB）12MiB
     */
    private static final int MEMORY_KIB = 12 * 1024; // Argon2PasswordEncoderはメモリをKiB単位で受け取る

    /**
     * 反復回数
     */
    private static final int ITERATIONS = 3;

    /**
     * Argon2idでハッシュ値を作り照合する部品
     */
    private final Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(
            SALT_LENGTH, HASH_LENGTH, PARALLELISM, MEMORY_KIB, ITERATIONS); // 引数の順番はソルト・ハッシュの長さ・並列度・メモリ・反復回数

    /**
     * アカウントがない場合の照合に使うダミーのハッシュ値
     */
    private final String dummyHash = encoder.encode("hoikulink-dummy-password"); // 部品を作るときに1回だけ計算しておく

    /**
     * 入力されたパスワードがハッシュ値と一致するかを返す
     * <p>ハッシュ値の文字列に書かれたパラメータとソルトで計算し直して比べる
     *
     * @param rawPassword 入力されたパスワード
     * @param passwordHash 保存されているパスワードのハッシュ値
     * @return 一致する場合は true
     */
    @Override
    public boolean matches(RawPassword rawPassword, PasswordHash passwordHash) {
        return encoder.matches(rawPassword.value(), passwordHash.value());
    }

    /**
     * ダミーのハッシュ値で照合し常にfalseを返す
     * <p>照合の結果は使わず処理時間をそろえるためだけに計算する
     *
     * @param rawPassword 入力されたパスワード
     * @return 常に false
     */
    @Override
    public boolean matchesDummy(RawPassword rawPassword) {
        encoder.matches(rawPassword.value(), dummyHash);
        return false;
    }
}
