package io.github.kawashima_study.hoikulink.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.kawashima_study.hoikulink.shared.UserId;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * ログインの失敗とロック（LoginLock）のテスト
 */
@DisplayName("ログインの失敗とロック")
class LoginLockTests {

    /** テストで使用する基準の日時 */
    private static final Instant NOW = Instant.parse("2026-10-16T00:00:00Z");

    /** ロックを解除した園長・主任のユーザーID */
    private static final UserId UNLOCKED_BY = UserId.newId();

    /**
     * 最初の状態が失敗0回でロックされていないことを確認
     */
    @Test
    @DisplayName("最初は失敗0回でロックされていない。")
    void startsUnlocked() {
        // 最初の状態を作成
        LoginLock lock = LoginLock.initial();

        // 失敗0回でロックされていないことを確認
        assertThat(lock.failedCount()).isZero();
        assertThat(lock.isLocked()).isFalse();
    }

    /**
     * 4回続けて失敗してもロックされないことを確認
     */
    @Test
    @DisplayName("4回続けて失敗してもまだロックされない。")
    void doesNotLockBeforeFifthFailure() {
        // 4回失敗
        LoginLock lock = failTimes(LoginLock.initial(), 4);

        // 失敗4回でロックされていないことを確認
        assertThat(lock.failedCount()).isEqualTo(4);
        assertThat(lock.isLocked()).isFalse();
    }

    /**
     * 5回続けて失敗するとその日時でロックされることを確認
     */
    @Test
    @DisplayName("5回続けて失敗したらその日時でロックされる。")
    void locksOnFifthFailure() {
        // 5回失敗
        LoginLock lock = failTimes(LoginLock.initial(), 5);

        // ロックされロック日時が5回目の日時であることを確認
        assertThat(lock.isLocked()).isTrue();
        assertThat(lock.lockedAt()).contains(NOW);
    }

    /**
     * ロック中に失敗しても失敗回数とロック日時が変わらないことを確認
     */
    @Test
    @DisplayName("ロック中に失敗しても失敗回数とロック日時は変わらない。")
    void keepsStateWhileLocked() {
        // ロック中の状態から1分後にもう一度失敗
        LoginLock locked = failTimes(LoginLock.initial(), 5);
        LoginLock afterMoreFailure = locked.recordFailure(NOW.plusSeconds(60));

        // 失敗回数は5のままロック日時は最初にロックした日時のままであることを確認
        assertThat(afterMoreFailure.failedCount()).isEqualTo(5);
        assertThat(afterMoreFailure.lockedAt()).contains(NOW);
    }

    /**
     * 失敗した日時が null の場合に例外になることを確認
     */
    @Test
    @DisplayName("失敗した日時がnullならNullPointerExceptionになる。")
    void rejectsNullFailedAt() {
        // 失敗した日時に null を渡すと NullPointerException になることを確認
        assertThatThrownBy(() -> LoginLock.initial().recordFailure(null)).isInstanceOf(NullPointerException.class);
    }

    /**
     * ログインに成功すると失敗回数が0に戻ることを確認
     */
    @Test
    @DisplayName("ログインに成功したら失敗回数を0に戻す。")
    void resetsCountOnSuccess() {
        // 3回失敗した後に成功
        LoginLock lock = failTimes(LoginLock.initial(), 3).recordSuccess();

        // 失敗0回でロックされていないことを確認
        assertThat(lock.failedCount()).isZero();
        assertThat(lock.isLocked()).isFalse();
    }

    /**
     * ロック中にログインの成功を記録すると例外になることを確認
     */
    @Test
    @DisplayName("ロック中はログインの成功を記録できない。")
    void rejectsSuccessWhileLocked() {
        // ロック中の状態を作成
        LoginLock locked = failTimes(LoginLock.initial(), 5);

        // 成功を記録すると IllegalStateException になることを確認
        assertThatThrownBy(locked::recordSuccess).isInstanceOf(IllegalStateException.class);
    }

    /**
     * ロックを解除すると失敗回数が0に戻り解除日時と解除者が記録されることを確認
     */
    @Test
    @DisplayName("ロックを解除したら失敗回数を0に戻し解除日時と解除者を記録する。")
    void unlockResetsCountAndRecordsUnlockedBy() {
        // ロック中の状態から1時間後に解除
        LoginLock locked = failTimes(LoginLock.initial(), 5);
        LoginLock unlocked = locked.unlock(NOW.plusSeconds(3600), UNLOCKED_BY);

        // ロックが外れ失敗0回になり解除日時と解除者が記録されていることを確認
        assertThat(unlocked.isLocked()).isFalse();
        assertThat(unlocked.failedCount()).isZero();
        assertThat(unlocked.unlockedAt()).contains(NOW.plusSeconds(3600));
        assertThat(unlocked.unlockedBy()).contains(UNLOCKED_BY);
    }

    /**
     * ロックされていないときに解除しても何も変わらないことを確認
     */
    @Test
    @DisplayName("ロックされていないときに解除しても何も変わらない。")
    void unlockDoesNothingWhenNotLocked() {
        // 2回失敗しただけのロックされていない状態で解除
        LoginLock lock = failTimes(LoginLock.initial(), 2);
        LoginLock afterUnlock = lock.unlock(NOW, UNLOCKED_BY);

        // 失敗回数は2のまま解除日時と解除者は記録されていないことを確認
        assertThat(afterUnlock.failedCount()).isEqualTo(2);
        assertThat(afterUnlock.unlockedAt()).isEmpty();
        assertThat(afterUnlock.unlockedBy()).isEmpty();
    }

    /**
     * 解除日時が null の場合に例外になることを確認
     */
    @Test
    @DisplayName("解除日時がnullならNullPointerExceptionになる。")
    void rejectsNullUnlockedAt() {
        // 解除日時に null を渡すと NullPointerException になることを確認
        assertThatThrownBy(() -> LoginLock.initial().unlock(null, UNLOCKED_BY))
                .isInstanceOf(NullPointerException.class);
    }

    /**
     * 解除者が null の場合に例外になることを確認
     */
    @Test
    @DisplayName("解除者がnullならNullPointerExceptionになる。")
    void rejectsNullUnlockedBy() {
        // 解除者に null を渡すと NullPointerException になることを確認
        assertThatThrownBy(() -> LoginLock.initial().unlock(NOW, null)).isInstanceOf(NullPointerException.class);
    }

    /**
     * 保存されているロックされていない状態を復元できることを確認
     */
    @Test
    @DisplayName("ロックされていない状態を復元できる。")
    void restoresUnlockedState() {
        // 失敗3回でロックなしの状態を復元
        LoginLock lock = LoginLock.restore(3, null, null, null);

        // 失敗3回でロックされていないことを確認
        assertThat(lock.failedCount()).isEqualTo(3);
        assertThat(lock.isLocked()).isFalse();
    }

    /**
     * 保存されているロック中の状態を復元できることを確認
     */
    @Test
    @DisplayName("ロック中の状態を復元できる。")
    void restoresLockedState() {
        // 失敗5回でロック日時ありの状態を復元
        LoginLock lock = LoginLock.restore(5, NOW, null, null);

        // ロックされていてロック日時が渡した日時であることを確認
        assertThat(lock.isLocked()).isTrue();
        assertThat(lock.lockedAt()).contains(NOW);
    }

    /**
     * 保存されている解除の記録がある状態を復元できることを確認
     */
    @Test
    @DisplayName("解除の記録がある状態を復元できる。")
    void restoresUnlockRecord() {
        // 失敗0回で解除日時と解除者ありの状態を復元
        LoginLock lock = LoginLock.restore(0, null, NOW, UNLOCKED_BY);

        // 解除日時と解除者が渡した値であることを確認
        assertThat(lock.unlockedAt()).contains(NOW);
        assertThat(lock.unlockedBy()).contains(UNLOCKED_BY);
    }

    /**
     * 矛盾した組み合わせを復元しようとすると例外になることを確認
     *
     * @param failedCount 連続したログインの失敗回数
     * @param lockedAt ロックした日時
     * @param unlockedAt 最後に解除した日時
     * @param unlockedBy 最後に解除した園長・主任のユーザーID
     */
    @ParameterizedTest
    @MethodSource("inconsistentStates")
    @DisplayName("矛盾した組み合わせは復元できない。")
    void rejectsInconsistentState(int failedCount, Instant lockedAt, Instant unlockedAt, UserId unlockedBy) {
        // 矛盾した組み合わせで復元すると IllegalArgumentException になることを確認
        assertThatThrownBy(() -> LoginLock.restore(failedCount, lockedAt, unlockedAt, unlockedBy))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 矛盾した組み合わせの一覧を返す
     * <p>並び順は失敗回数 ロック日時 解除日時 解除者
     *
     * @return 矛盾した組み合わせの一覧
     */
    static Stream<Arguments> inconsistentStates() {
        return Stream.of(
                // 5回なのにロック日時がない
                Arguments.of(5, null, null, null),
                // 0回なのにロック日時がある
                Arguments.of(0, NOW, null, null),
                // 4回なのにロック日時がある
                Arguments.of(4, NOW, null, null),
                // 失敗回数が0より小さい
                Arguments.of(-1, null, null, null),
                // 失敗回数が5より大きい
                Arguments.of(6, NOW, null, null),
                // 解除日時のみがある
                Arguments.of(0, null, NOW, null),
                // 解除者のみがある
                Arguments.of(0, null, null, UNLOCKED_BY));
    }

    /**
     * 指定した回数だけログインの失敗を記録した状態を返す
     *
     * @param lock 元の状態
     * @param times 失敗させる回数
     * @return 失敗を記録した後の状態
     */
    private static LoginLock failTimes(LoginLock lock, int times) {
        LoginLock result = lock;
        for (int i = 0; i < times; i++) {
            // 新しい LoginLock が返るため変数に入れ直す
            result = result.recordFailure(NOW);
        }
        return result;
    }
}
