package io.github.kawashima_study.hoikulink.auth.application;

/**
 * ログアウトのユースケースの入口
 */
public interface LogoutUseCase {

    /**
     * 同じログインのリフレッシュトークンをすべて無効化する
     * <p>トークンが見つからない場合も何もせずに終わる（何度呼んでも同じ結果になる）
     *
     * @param command ログアウトの入力
     */
    void logout(LogoutCommand command);
}
