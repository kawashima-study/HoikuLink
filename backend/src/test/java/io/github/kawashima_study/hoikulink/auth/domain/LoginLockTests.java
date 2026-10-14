package io.github.kawashima_study.hoikulink.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.kawashima_study.hoikulink.shared.UserId;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ログインの失敗とロック")
class LoginLockTests {

    private static final Instant NOW = Instant.parse("2026-10-11T00:00:00Z");

    @Test
    @DisplayName("最初は、失敗0回でロックされていない")
    void startsUnlocked() {
        LoginLock lock = LoginLock.initial();

        assertThat(lock.failedCount()).isZero();
        assertThat(lock.isLocked()).isFalse();
    }

    @Test
    @DisplayName("4回続けて失敗しても、まだロックされない")
    void doesNotLockBeforeFifthFailure() {
        LoginLock lock = failTimes(LoginLock.initial(), 4);

        assertThat(lock.failedCount()).isEqualTo(4);
        assertThat(lock.isLocked()).isFalse();
    }

    @Test
    @DisplayName("5回続けて失敗したら、その時刻でロックされる")
    void locksOnFifthFailure() {
        LoginLock lock = failTimes(LoginLock.initial(), 5);

        assertThat(lock.isLocked()).isTrue();
        assertThat(lock.lockedAt()).contains(NOW);
    }

    @Test
    @DisplayName("ロック中に失敗しても、ロックの時刻は変わらない")
    void keepsLockedAtWhileLocked() {
        LoginLock locked = failTimes(LoginLock.initial(), 5);

        LoginLock afterMoreFailure = locked.recordFailure(NOW.plusSeconds(60));

        assertThat(afterMoreFailure.lockedAt()).contains(NOW);
    }

    @Test
    @DisplayName("ログインに成功したら、失敗の回数を0に戻す")
    void resetsCountOnSuccess() {
        LoginLock lock = failTimes(LoginLock.initial(), 3).recordSuccess();

        assertThat(lock.failedCount()).isZero();
        assertThat(lock.isLocked()).isFalse();
    }

    @Test
    @DisplayName("ロックを解除したら、失敗の回数を0に戻し、解除した時刻と人を残す")
    void unlockResetsCountAndRecordsWhoUnlocked() {
        UserId director = UserId.newId();
        LoginLock locked = failTimes(LoginLock.initial(), 5);

        LoginLock unlocked = locked.unlock(NOW.plusSeconds(3600), director);

        assertThat(unlocked.isLocked()).isFalse();
        assertThat(unlocked.failedCount()).isZero();
        assertThat(unlocked.unlockedAt()).contains(NOW.plusSeconds(3600));
        assertThat(unlocked.unlockedBy()).contains(director);
    }

    private static LoginLock failTimes(LoginLock lock, int times) {
        LoginLock result = lock;
        for (int i = 0; i < times; i++) {
            result = result.recordFailure(NOW);
        }
        return result;
    }
}
