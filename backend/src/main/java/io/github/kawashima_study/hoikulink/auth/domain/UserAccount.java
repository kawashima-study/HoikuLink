package io.github.kawashima_study.hoikulink.auth.domain;

import io.github.kawashima_study.hoikulink.shared.UserId;
import java.time.Instant;
import java.util.Objects;

/**
 * アカウント
 * <p>ログインの失敗、成功の記録とロックの解除を受け付ける
 */
public final class UserAccount {

    /** ユーザーID */
    private final UserId userId;

    /** ログインID（メールアドレス） */
    private final EmailAddress emailAddress;

    /** パスワードのハッシュ値 */
    private final PasswordHash passwordHash;

    /** ログインの失敗回数とロックの状態 */
    private LoginLock loginLock; // あとで新しいLoginLockに入れ替えるためfinalを付けない

    /**
     * アカウントを作成する
     * <p>外からはrestoreを使って作成する
     *
     * @param userId ユーザーID
     * @param emailAddress ログインID（メールアドレス）
     * @param passwordHash パスワードのハッシュ値
     * @param loginLock ログインの失敗回数とロックの状態
     */
    private UserAccount(UserId userId, EmailAddress emailAddress, PasswordHash passwordHash, LoginLock loginLock) {
        this.userId = userId;
        this.emailAddress = emailAddress;
        this.passwordHash = passwordHash;
        this.loginLock = loginLock;
    }

    /**
     * 保存されている値からアカウントを作成する
     *
     * @param userId ユーザーID
     * @param emailAddress ログインID（メールアドレス）
     * @param passwordHash パスワードのハッシュ値
     * @param loginLock ログインの失敗回数とロックの状態
     * @return アカウント
     * @throws NullPointerException いずれかの引数がnullの場合
     */
    public static UserAccount restore(
            UserId userId, EmailAddress emailAddress, PasswordHash passwordHash, LoginLock loginLock) {
        Objects.requireNonNull(userId, "ユーザーIDがnullです。");
        Objects.requireNonNull(emailAddress, "ログインIDがnullです。");
        Objects.requireNonNull(passwordHash, "パスワードのハッシュ値がnullです。");
        Objects.requireNonNull(loginLock, "ログイン失敗回数とロック状態がnullです。");
        return new UserAccount(userId, emailAddress, passwordHash, loginLock);
    }

    /**
     * ログインの失敗を記録する
     * <p>5回目の失敗でロックする ロック中は変更しない
     *
     * @param failedAt 失敗した日時
     * @throws NullPointerException 失敗した日時が null の場合
     */
    public void recordLoginFailure(Instant failedAt) { // 自分の中身を変えるので戻り値はvoid
        this.loginLock = loginLock.recordFailure(failedAt); // LoginLockに失敗を記録させ、返ってきた新しいLoginLockに入れ替える
        // nullの確認と5回でロックする判定はLoginLock.recordFailureにて行う
    }

    /**
     * ログインの成功を記録して失敗回数を0に戻す
     *
     * @throws IllegalStateException ロック中の場合
     */
    public void recordLoginSuccess() {
        this.loginLock = loginLock.recordSuccess();
    }

    /**
     * ロックを解除する
     * <p>失敗回数を0に戻し解除した日時と解除者を記録する
     * <p>ロックされていないときは変更しない
     *
     * @param unlockedAt 解除した日時
     * @param unlockedBy 解除した園長または主任のユーザーID
     * @throws NullPointerException 解除した日時または解除者がnullの場合
     * @throws SelfUnlockNotAllowedException 解除者が本人の場合
     */
    public void unlock(Instant unlockedAt, UserId unlockedBy) {
        if (userId.equals(unlockedBy)) { // アカウントのユーザーIDと解除者のユーザーIDが同じか比べる
            throw new SelfUnlockNotAllowedException(); // 同一人物の場合ロックの状態は変更せず、例外
        }
        this.loginLock = loginLock.unlock(unlockedAt, unlockedBy);
    }

    /**
     * ロック中かどうかを返す
     *
     * @return ロック中の場合は true
     */
    public boolean isLocked() {
        return loginLock.isLocked();
    }

    /**
     * ユーザーIDを返す
     *
     * @return ユーザーID
     */
    public UserId userId() {
        return userId;
    }

    /**
     * ログインID（メールアドレス）を返す
     *
     * @return ログインID（メールアドレス）
     */
    public EmailAddress emailAddress() {
        return emailAddress;
    }

    /**
     * パスワードのハッシュ値を返す
     *
     * @return パスワードのハッシュ値
     */
    public PasswordHash passwordHash() {
        return passwordHash;
    }

    /**
     * ログインの失敗回数とロックの状態を返す
     *
     * @return ログインの失敗回数とロックの状態
     */
    public LoginLock loginLock() {
        return loginLock;
    }
}
