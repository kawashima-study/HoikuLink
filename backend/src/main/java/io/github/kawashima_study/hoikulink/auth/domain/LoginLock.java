package io.github.kawashima_study.hoikulink.auth.domain;

import io.github.kawashima_study.hoikulink.shared.UserId;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * ログインの失敗回数とロックの状態
 * <p>中身は変更せず 変更するときは新しいインスタンスを返す
 */
public final class LoginLock {

    /** ロックするログイン失敗回数 */
    private static final int MAX_FAILED_COUNT = 5;

    /** ログイン失敗回数 */
    private final int failedCount;

    /** ロックした日時 */
    private final Instant lockedAt;

    /** 最後に解除した日時 */
    private final Instant unlockedAt;

    /** 最後に解除した園長・主任のユーザーID */
    private final UserId unlockedBy;

    /**
     * 状態を作成
     * <p>外部からはinitialまたはrestoreを使用する
     *
     * @param failedCount ログイン失敗回数
     * @param lockedAt ロックした日時
     * @param unlockedAt 最後に解除した日時
     * @param unlockedBy 最後に解除した園長・主任のユーザーID
     */
    private LoginLock(int failedCount, Instant lockedAt, Instant unlockedAt, UserId unlockedBy) {
        this.failedCount = failedCount;
        this.lockedAt = lockedAt;
        this.unlockedAt = unlockedAt;
        this.unlockedBy = unlockedBy;
    }

    /**
     * 最初の状態を作成
     *
     * @return 失敗回数0でロックされていない状態
     */
    public static LoginLock initial() {
        return new LoginLock(0, null, null, null);
    }

    /**
     * 保存されている値から状態を復元
     *
     * @param failedCount ログイン失敗回数
     * @param lockedAt ロックした日時
     * @param unlockedAt 最後に解除した日時
     * @param unlockedBy 最後に解除した園長・主任のユーザーID
     * @return 復元した状態
     * @throws IllegalArgumentException 値の組み合わせが矛盾している場合
     */
    public static LoginLock restore(int failedCount, Instant lockedAt, Instant unlockedAt, UserId unlockedBy) {
        requireValidLockState(failedCount, lockedAt);
        requireValidUnlockRecord(unlockedAt, unlockedBy);
        return new LoginLock(failedCount, lockedAt, unlockedAt, unlockedBy);
    }

    /**
     * ログインの失敗を記録
     * <p>5回目の失敗でロックする
     * <p>ロック中は変更しない
     *
     * @param failedAt 失敗した日時
     * @return 失敗を記録した後の状態
     * @throws NullPointerException 失敗した日時がnullの場合
     */
    public LoginLock recordFailure(Instant failedAt) {
        Objects.requireNonNull(failedAt, "失敗した日時がnullです。");
        if (isLocked()) {
            return this;
        }
        int nextFailedCount = failedCount + 1;
        Instant nextLockedAt = nextFailedCount == MAX_FAILED_COUNT ? failedAt : null;
        return new LoginLock(nextFailedCount, nextLockedAt, unlockedAt, unlockedBy);
    }

    /**
     * ログインの成功を記録
     *
     * @return 失敗回数を0にした状態
     * @throws IllegalStateException ロック中の場合
     */
    public LoginLock recordSuccess() {
        if (isLocked()) {
            throw new IllegalStateException("ロック中のためログインの成功を記録できません。");
        }
        return new LoginLock(0, null, unlockedAt, unlockedBy);
    }

    /**
     * ロックを解除
     * <p>失敗回数を0にして解除日時と解除者を記録する
     * <p>ロックされていない場合は変更しない
     *
     * @param unlockedAt 解除した日時
     * @param unlockedBy 解除した園長・主任のユーザーID
     * @return 解除した後の状態
     * @throws NullPointerException 解除した日時または解除者がnullの場合
     */
    public LoginLock unlock(Instant unlockedAt, UserId unlockedBy) {
        Objects.requireNonNull(unlockedAt, "解除した日時がnullです。");
        Objects.requireNonNull(unlockedBy, "解除者がnullです。");
        if (!isLocked()) {
            return this;
        }
        return new LoginLock(0, null, unlockedAt, unlockedBy);
    }

    /**
     * ロック中かを返す
     *
     * @return ロック日時がある場合 true
     */
    public boolean isLocked() {
        return lockedAt != null;
    }

    /**
     * 連続したログインの失敗回数を返す
     *
     * @return 失敗回数
     */
    public int failedCount() {
        return failedCount;
    }

    /**
     * ロックした日時を返す
     *
     * @return ロックした日時 ※ロックされていない場合は空
     */
    public Optional<Instant> lockedAt() {
        return Optional.ofNullable(lockedAt);
    }

    /**
     * 最後に解除した日時を返す
     *
     * @return 最後に解除した日時 ※解除の記録がない場合は空
     */
    public Optional<Instant> unlockedAt() {
        return Optional.ofNullable(unlockedAt);
    }

    /**
     * 最後に解除した園長・主任のユーザーIDを返す
     *
     * @return 解除者のユーザーID ※解除の記録がない場合は空
     */
    public Optional<UserId> unlockedBy() {
        return Optional.ofNullable(unlockedBy);
    }

    /**
     * 失敗回数とロック日時の組み合わせを確認
     *
     * @param failedCount ログイン失敗回数
     * @param lockedAt ロックした日時
     * @throws IllegalArgumentException 失敗回数が範囲外の場合または失敗回数とロック日時が矛盾する場合
     */
    private static void requireValidLockState(int failedCount, Instant lockedAt) {
        boolean locked = lockedAt != null;
        if (failedCount < 0 || failedCount > MAX_FAILED_COUNT || locked != (failedCount == MAX_FAILED_COUNT)) {
            throw new IllegalArgumentException("失敗回数とロック日時の組み合わせが正しくありません。");
        }
    }

    /**
     * 解除日時と解除者の組み合わせを確認
     *
     * @param unlockedAt 最後に解除した日時
     * @param unlockedBy 最後に解除した園長・主任のユーザーID
     * @throws IllegalArgumentException 片方のみに値がある場合
     */
    private static void requireValidUnlockRecord(Instant unlockedAt, UserId unlockedBy) {
        if ((unlockedAt == null) != (unlockedBy == null)) {
            throw new IllegalArgumentException("解除日時と解除者の組み合わせが正しくありません。");
        }
    }
}
