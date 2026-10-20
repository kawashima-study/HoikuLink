package io.github.kawashima_study.hoikulink.auth.domain;

/**
 * 入力されたパスワードとパスワードのハッシュ値を照合するインターフェース
 * <p>照合の方法（Argon2id）は知らず メソッドの形だけを決める
 * <p>実装はadapterの中に置く
 */
public interface PasswordVerifier {

    /**
     * 入力されたパスワードがハッシュ値と一致するかを返す
     *
     * @param rawPassword 入力されたパスワード
     * @param passwordHash 保存されているパスワードのハッシュ値
     * @return 一致する場合は true
     */
    boolean matches(RawPassword rawPassword, PasswordHash passwordHash);

    /**
     * ダミーのハッシュ値で照合し常に false を返す
     * <p>アカウントがない場合に照合の処理時間をそろえアカウントの有無を処理時間から分からなくするために使う
     *
     * @param rawPassword 入力されたパスワード
     * @return 常に false
     */
    boolean matchesDummy(RawPassword rawPassword); // 照合はするが結果は使わない（時間をかけることが目的のため）
}
