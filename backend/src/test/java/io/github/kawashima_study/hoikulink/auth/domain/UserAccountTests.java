package io.github.kawashima_study.hoikulink.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.kawashima_study.hoikulink.shared.UserId;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * アカウント（UserAccount）のテスト
 */
@DisplayName("アカウント（UserAccount）")
class UserAccountTests {

    /** テストで使用する基準の日時 */
    private static final Instant NOW = Instant.parse("2026-10-16T00:00:00Z");

    /** ロックを解除した園長・主任のユーザーID */
    private static final UserId UNLOCKED_BY = UserId.newId();

    /**
     * 5回続けてログインに失敗するとロックされることを確認
     */
    @Test
    @DisplayName("5回続けてログインに失敗したらロックされる。")
    void locksAfterFiveFailures() {
        // 5回失敗
        UserAccount account = lockedAccount();

        // ロックされていることを確認
        assertThat(account.isLocked()).isTrue();
    }

    /**
     * ログインに成功すると失敗回数が0に戻ることを確認
     */
    @Test
    @DisplayName("ログインに成功したら失敗の回数を0に戻す。")
    void resetsFailuresOnSuccess() {
        // 1回失敗した後に成功
        UserAccount account = newAccount();
        account.recordLoginFailure(NOW);
        account.recordLoginSuccess();

        // 失敗0回であることを確認
        assertThat(account.loginLock().failedCount()).isZero();
    }

    /**
     * ロック中にログインの成功を記録すると例外になることを確認
     */
    @Test
    @DisplayName("ロック中はログインの成功として扱えない。")
    void rejectsSuccessWhileLocked() {
        // ロック中のアカウントを作成
        UserAccount account = lockedAccount();

        // 成功を記録すると IllegalStateException になることを確認
        assertThatThrownBy(account::recordLoginSuccess).isInstanceOf(IllegalStateException.class);
    }

    /**
     * ロックを解除するとログインできる状態に戻ることを確認
     */
    @Test
    @DisplayName("ロックを解除したらログインできる状態に戻る。")
    void unlockAllowsLoginAgain() {
        // ロック中のアカウントを1分後に解除
        UserAccount account = lockedAccount();
        account.unlock(NOW.plusSeconds(60), UNLOCKED_BY);

        // ロックされていないことを確認
        assertThat(account.isLocked()).isFalse();
    }

    /**
     * 解除日時が null の場合に例外になることを確認
     */
    @Test
    @DisplayName("解除日時がnullならNullPointerExceptionになる。")
    void rejectsNullUnlockedAt() {
        // ロック中のアカウントを作成
        UserAccount account = lockedAccount();

        // 解除日時に null を渡すと NullPointerException になることを確認
        assertThatThrownBy(() -> account.unlock(null, UNLOCKED_BY)).isInstanceOf(NullPointerException.class);
    }

    /**
     * 自分自身のロックを解除すると例外になりロックされたままであることを確認
     */
    @Test
    @DisplayName("自分自身のロックは解除できない。")
    void rejectsSelfUnlock() {
        // ロック中のアカウントを作成
        UserAccount account = lockedAccount();

        // 解除者にアカウント自身のユーザーIDを渡すと SelfUnlockNotAllowedException になることを確認
        assertThatThrownBy(() -> account.unlock(NOW.plusSeconds(60), account.userId()))
                .isInstanceOf(SelfUnlockNotAllowedException.class);

        // 例外になった後もロックされたままであることを確認
        assertThat(account.isLocked()).isTrue();
    }

    /**
     * ロックされていなくても自分自身を解除者にすると例外になることを確認
     */
    @Test
    @DisplayName("ロックされていなくても自分自身を解除者にはできない。")
    void rejectsSelfUnlockWhenNotLocked() {
        // ロックされていないアカウントを作成
        UserAccount account = newAccount();

        // 解除者にアカウント自身のユーザーIDを渡すと SelfUnlockNotAllowedException になることを確認
        assertThatThrownBy(() -> account.unlock(NOW, account.userId()))
                .isInstanceOf(SelfUnlockNotAllowedException.class);
    }

    /**
     * 失敗0回でロックされていないアカウントを作成
     *
     * @return 新しいアカウント
     */
    private static UserAccount newAccount() {
        return UserAccount.restore(
                UserId.newId(),
                new EmailAddress("hanako@example.com"),
                new PasswordHash("$argon2id$dummy"),
                LoginLock.initial());
    }

    /**
     * 5回続けてログインに失敗したロック中のアカウントを作成
     *
     * @return ロック中のアカウント
     */
    private static UserAccount lockedAccount() {
        UserAccount account = newAccount();
        for (int i = 0; i < 5; i++) {
            account.recordLoginFailure(NOW);
        }
        return account;
    }
}
