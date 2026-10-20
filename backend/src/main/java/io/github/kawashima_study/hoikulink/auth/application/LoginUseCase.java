package io.github.kawashima_study.hoikulink.auth.application;

/**
 * ログインのユースケースの入口
 * <p>APIの側はこのインターフェースだけを使い実装のクラスには依存しない
 */
public interface LoginUseCase {

    /**
     * メールアドレスとパスワードでログインする
     *
     * @param command ログインの入力
     * @return ログインの結果（成功・失敗・ロック中のいずれか）
     */
    LoginResult login(LoginCommand command);
}
