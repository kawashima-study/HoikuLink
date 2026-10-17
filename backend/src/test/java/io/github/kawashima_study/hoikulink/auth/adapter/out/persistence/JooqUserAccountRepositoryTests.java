package io.github.kawashima_study.hoikulink.auth.adapter.out.persistence;

import static io.github.kawashima_study.hoikulink.auth.adapter.out.jooq.generated.Tables.CREDENTIALS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.kawashima_study.hoikulink.TestcontainersConfiguration;
import io.github.kawashima_study.hoikulink.auth.adapter.out.jooq.generated.tables.records.CredentialsRecord;
import io.github.kawashima_study.hoikulink.auth.domain.EmailAddress;
import io.github.kawashima_study.hoikulink.auth.domain.LoginLock;
import io.github.kawashima_study.hoikulink.auth.domain.PasswordHash;
import io.github.kawashima_study.hoikulink.auth.domain.UserAccount;
import io.github.kawashima_study.hoikulink.auth.domain.UserAccountRepository;
import io.github.kawashima_study.hoikulink.shared.UserId;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * アカウントのRepository（JooqUserAccountRepository）のテスト
 * <p>Testcontainersで起動した本物のMySQLに読み書きして確認する
 */
@SpringBootTest // アプリ全体を起動してテストする（Flywayでテーブルも作られる）
@Import(TestcontainersConfiguration.class) // テスト用のMySQL（Docker）を起動する設定を読み込む
@Transactional // テストごとにトランザクションを始め 終わったら取り消す（入れたデータが次のテストに残らない）
@DisplayName("アカウントのRepository")
class JooqUserAccountRepositoryTests {

    /** テストのデータを入れたときの日時（日本時間） */
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 10, 16, 9, 0);

    /** CREATED_ATと同じ時点をInstantで表した日時（日本時間の9時はUTCの0時） */
    private static final Instant NOW = Instant.parse("2026-10-16T00:00:00Z");

    /** メールアドレス（テストのデータ） */
    private static final String EMAIL_ADDRESS = "hanako@example.com";

    /** パスワードのハッシュ値（テストのデータ） */
    private static final String PASSWORD_HASH = "$argon2id$dummy";

    /** テスト対象のRepository */
    @Autowired // Springが作った部品をここに入れてもらう（テストのクラスではフィールドに書く）
    private UserAccountRepository repository; // 実装のクラスではなくinterface型で受け取る

    /** テストのデータを直接入れるためのjOOQの入口 */
    @Autowired
    private DSLContext dsl;

    /**
     * メールアドレスで保存されているアカウントを読み込めることを確認
     */
    @Test
    @DisplayName("メールアドレスで保存されているアカウントを読み込める。")
    void findsAccountByEmail() {
        // ロックされていないアカウントをDBに挿入
        UserId userId = UserId.newId();
        insertAccount(userId, EMAIL_ADDRESS);

        // メールアドレスで読み込む
        Optional<UserAccount> found = repository.findByEmailForUpdate(new EmailAddress(EMAIL_ADDRESS));

        // 対象レコードが見つかり、挿入したレコードであることを確認
        assertThat(found).isPresent(); // 空ではないことを確認
        UserAccount account = found.get(); // Optionalの中身を取り出す（空のときに呼ぶと例外になるため、先にisPresentで確認）
        assertThat(account.userId()).isEqualTo(userId);
        assertThat(account.emailAddress()).isEqualTo(new EmailAddress(EMAIL_ADDRESS));
        assertThat(account.loginLock().failedCount()).isZero();
        assertThat(account.isLocked()).isFalse();
    }

    /**
     * 登録されていないメールアドレスでは何も見つからないことを確認
     */
    @Test
    @DisplayName("登録されていないメールアドレスでは何も見つからない。")
    void findsNothingForUnknownEmail() {
        // 未挿入のメールアドレスで読み込む
        Optional<UserAccount> found = repository.findByEmailForUpdate(new EmailAddress(EMAIL_ADDRESS));

        // 対象レコードが見つからないことを確認
        assertThat(found).isEmpty();
    }

    /**
     * ユーザーIDで保存されているアカウントを読み込めることを確認
     */
    @Test
    @DisplayName("ユーザーIDで保存されているアカウントを読み込める。")
    void findsAccountById() {
        // ロックされていないアカウントをDBに挿入
        UserId userId = UserId.newId();
        insertAccount(userId, EMAIL_ADDRESS);

        // ユーザーIDで読み込む
        Optional<UserAccount> found = repository.findByIdForUpdate(userId);

        // 対象レコードが見つかり、挿入したレコードであることを確認
        assertThat(found).isPresent(); // 空ではないことを確認
        UserAccount account = found.get(); // Optionalの中身を取り出す（空のときに呼ぶと例外になるため、先にisPresentで確認）
        assertThat(account.userId()).isEqualTo(userId);
        assertThat(account.emailAddress()).isEqualTo(new EmailAddress(EMAIL_ADDRESS));
        assertThat(account.loginLock().failedCount()).isZero();
        assertThat(account.isLocked()).isFalse();
    }

    /**
     * 削除済みのアカウントは読み込まないことを確認
     */
    @Test
    @DisplayName("削除済みのアカウントは読み込まない。")
    void ignoresDeletedAccount() {
        // 削除アカウントをDBに挿入
        UserId userId = UserId.newId();
        insertAccount(userId, EMAIL_ADDRESS);
        dsl.update(CREDENTIALS)
                .set(CREDENTIALS.DELETED_AT, CREATED_AT)
                .where(CREDENTIALS.USER_ID.eq(userId.value()))
                .execute();

        // ユーザーIDで読み込む
        Optional<UserAccount> found = repository.findByIdForUpdate(userId);

        // 対象レコードが見つからないことを確認
        assertThat(found).isEmpty();
    }

    /**
     * ロック中のアカウントはロックされた状態で読み込めることを確認
     */
    @Test
    @DisplayName("ロック中のアカウントはロックされた状態で読み込める。")
    void restoresLockState() {
        // アカウントをDBに挿入しロック状態に更新
        UserId userId = UserId.newId();
        insertAccount(userId, EMAIL_ADDRESS);
        lockAccount(userId);

        // ユーザーIDで読み込む
        Optional<UserAccount> found = repository.findByIdForUpdate(userId);

        // 対象レコードが見つかり、更新したレコードであることを確認
        assertThat(found).isPresent(); // 空ではないことを確認
        UserAccount account = found.get(); // Optionalの中身を取り出す（空のときに呼ぶと例外になるため、先にisPresentで確認）
        assertThat(account.userId()).isEqualTo(userId);
        assertThat(account.emailAddress()).isEqualTo(new EmailAddress(EMAIL_ADDRESS));
        assertThat(account.loginLock().failedCount()).isEqualTo(5);
        assertThat(account.isLocked()).isTrue();
        assertThat(account.loginLock().lockedAt())
                .isEqualTo(Optional.of(NOW)); // DBの日本時間の9時がInstantのUTCの0時に変換されていることを確認
    }

    /**
     * 書き込んだ失敗回数とロックの状態をDBに保存することを確認
     */
    @Test
    @DisplayName("書き込んだ失敗回数とロックの状態をDBに保存する。")
    void updatesLockState() {
        // ロックされていないアカウントをDBに挿入して読み込む
        UserId userId = UserId.newId();
        insertAccount(userId, EMAIL_ADDRESS);
        UserAccount account = repository.findByIdForUpdate(userId).orElseThrow(); // 中身があれば取り出し、空なら例外にする（見つかる前提のときに使う）

        // 5回失敗させてロックし1時間後の日時で書き込む
        for (int i = 0; i < 5; i++) {
            account.recordLoginFailure(NOW);
        }
        repository.update(account, userId, NOW.plusSeconds(3600)); // 更新日時は挿入時と区別できるように1時間後にする

        // DBのレコードを直接読み出して書き込まれた値を確認
        CredentialsRecord row = dsl.selectFrom(CREDENTIALS) // SELECT * FROM auth.credentials と同じ意味
                .where(CREDENTIALS.USER_ID.eq(userId.value()))
                .fetchSingle(); // 1件だけ読み出す
        assertThat(row.getFailedLoginCount()).isEqualTo((byte) 5); // 失敗回数の列はByte型のため(byte)を付けて比べる
        assertThat(row.getLockedAt()).isEqualTo(CREATED_AT); // ロック日時（UTCの0時）が日本時間の9時で保存されていることを確認
        assertThat(row.getUpdatedAt()).isEqualTo(CREATED_AT.plusHours(1)); // 更新日時が日本時間の10時で保存されていることを確認
        assertThat(row.getUpdatedBy()).isEqualTo(userId.value()); // 更新者がアカウント自身のユーザーIDであることを確認
    }

    /**
     * DBにないアカウントを書き込もうとすると例外になることを確認
     */
    @Test
    @DisplayName("DBにないアカウントを書き込もうとすると例外になる。")
    void rejectsUpdateOfMissingAccount() {
        // DBに挿入せずにアカウントだけを作成
        UserAccount account = UserAccount.restore(
                UserId.newId(), new EmailAddress(EMAIL_ADDRESS), new PasswordHash(PASSWORD_HASH), LoginLock.initial());

        // 書き込むとIllegalStateExceptionになることを確認
        assertThatThrownBy(() -> repository.update(account, account.userId(), NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * 失敗0回でロックされていないアカウントをDBに挿入
     *
     * @param userId ユーザーID
     * @param email メールアドレス（小文字）
     */
    private void insertAccount(UserId userId, String email) {
        dsl.insertInto(CREDENTIALS) // INSERT INTO auth.credentialsと同じ意味（CREDENTIALSはjOOQが作ったテーブルの型）
                .set(CREDENTIALS.USER_ID, userId.value())
                .set(CREDENTIALS.EMAIL, email)
                .set(CREDENTIALS.PASSWORD_HASH, PASSWORD_HASH)
                .set(CREDENTIALS.CREATED_AT, CREATED_AT)
                .set(CREDENTIALS.CREATED_BY, userId.value())
                .set(CREDENTIALS.UPDATED_AT, CREATED_AT)
                .set(CREDENTIALS.UPDATED_BY, userId.value())
                .execute(); // ここで初めてSQLが実行される（失敗回数などデフォルト値のある列は書かなくてよい）
    }

    /**
     * DBのアカウントを失敗5回のロック中の状態に更新
     * <p>ロック日時は日本時間の2026-10-16 9時とする
     *
     * @param userId ユーザーID
     */
    private void lockAccount(UserId userId) {
        dsl.update(CREDENTIALS)
                .set(CREDENTIALS.FAILED_LOGIN_COUNT, (byte) 5) // 失敗回数の列はByte型のため(byte)を付ける
                .set(CREDENTIALS.LOCKED_AT, CREATED_AT) // ロック日時の列はLocalDateTime型（日本時間）のためそのまま入れる
                .where(CREDENTIALS.USER_ID.eq(userId.value()))
                .execute();
    }
}
