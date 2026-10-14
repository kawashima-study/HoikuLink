package io.github.kawashima_study.hoikulink.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.kawashima_study.hoikulink.shared.UserId;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("アカウント（UserAccount）")
class UserAccountTests {

    private static final Instant NOW = Instant.parse("2026-10-11T00:00:00Z");

    private UserAccount newAccount() {
        return UserAccount.restore(
                UserId.newId(), new EmailAddress("hanako@example.com"), new PasswordHash("$argon2id$dummy"), LoginLock.initial());
    }

    @Test
    @DisplayName("5回続けてログインに失敗したら、ロックされる")
    void locksAfterFiveFailures() {
        UserAccount account = newAccount();

        for (int i = 0; i < 5; i++) {
            account.recordLoginFailure(NOW);
        }

        assertThat(account.isLocked()).isTrue();
    }

    @Test
    @DisplayName("ログインに成功したら、失敗の回数を0に戻す")
    void resetsFailuresOnSuccess() {
        UserAccount account = newAccount();
        account.recordLoginFailure(NOW);

        account.recordLoginSuccess();

        assertThat(account.loginLock().failedCount()).isZero();
    }

    @Test
    @DisplayName("ロック中は、ログインの成功として扱えない")
    void rejectsSuccessWhileLocked() {
        UserAccount account = newAccount();
        for (int i = 0; i < 5; i++) {
            account.recordLoginFailure(NOW);
        }

        assertThatThrownBy(account::recordLoginSuccess).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("ロックを解除したら、ログインできる状態に戻る")
    void unlockAllowsLoginAgain() {
        UserAccount account = newAccount();
        for (int i = 0; i < 5; i++) {
            account.recordLoginFailure(NOW);
        }

        account.unlock(NOW.plusSeconds(60), UserId.newId());

        assertThat(account.isLocked()).isFalse();
    }
}
