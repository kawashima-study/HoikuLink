package io.github.kawashima_study.hoikulink.auth.adapter.out.persistence;

import static io.github.kawashima_study.hoikulink.auth.adapter.out.jooq.generated.Tables.CREDENTIALS;

import io.github.kawashima_study.hoikulink.auth.adapter.out.jooq.generated.tables.records.CredentialsRecord;
import io.github.kawashima_study.hoikulink.auth.domain.EmailAddress;
import io.github.kawashima_study.hoikulink.auth.domain.LoginLock;
import io.github.kawashima_study.hoikulink.auth.domain.PasswordHash;
import io.github.kawashima_study.hoikulink.auth.domain.UserAccount;
import io.github.kawashima_study.hoikulink.auth.domain.UserAccountRepository;
import io.github.kawashima_study.hoikulink.shared.BusinessTimeZone;
import io.github.kawashima_study.hoikulink.shared.UserId;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/**
 * アカウントの保存と読み出しのjOOQでの実装
 * <p>テーブルはauth.credentialsを使う
 */
@Repository // DBを扱う部品としてSpringにこのクラスのBeanを作らせる
class JooqUserAccountRepository implements UserAccountRepository { // 外のモジュールからはinterfaceだけを使うためpublicは付けない

    /** SQLを組み立てて実行するjOOQの入口 */
    private final DSLContext dsl; // SQLはすべてこれを使って組み立てる

    /**
     * Repositoryを作成
     *
     * @param dsl SQLを組み立てて実行するjOOQの入口
     */
    JooqUserAccountRepository(DSLContext dsl) { // コンストラクタの引数に書くとSpringが自動で渡す（コンストラクタインジェクション）
        this.dsl = dsl;
    }

    /**
     * メールアドレスでアカウントを探しその行をロックする
     *
     * @param emailAddress ログインID（メールアドレス）
     * @return 見つかったアカウント（見つからない場合は空）
     */
    @Override // interfaceのメソッドの実装であることを表す
    public Optional<UserAccount> findByEmailForUpdate(EmailAddress emailAddress) {
        return dsl.selectFrom(CREDENTIALS)
                .where(CREDENTIALS.EMAIL.eq(emailAddress.value()))
                .and(CREDENTIALS.DELETED_AT.isNull())
                .forUpdate() // FOR UPDATE（読んだ行をトランザクションの終わりまでロックする）
                .fetchOptional() // ここでSQLを実行し、0件なら空のOptional、1件ならその行が入ったOptionalを返す
                .map(this::toUserAccount); // 中身があるときだけtoUserAccountでUserAccountに変換する（空ならそのまま空）
    }

    /**
     * ユーザーIDでアカウントを探しその行をロックする
     *
     * @param userId ユーザーID
     * @return 見つかったアカウント（見つからない場合は空）
     */
    @Override
    public Optional<UserAccount> findByIdForUpdate(UserId userId) {
        return dsl.selectFrom(CREDENTIALS)
                .where(CREDENTIALS.USER_ID.eq(userId.value()))
                .and(CREDENTIALS.DELETED_AT.isNull())
                .forUpdate()
                .fetchOptional()
                .map(this::toUserAccount);
    }

    /**
     * アカウントのログインの失敗回数とロックの状態を書き込む
     *
     * @param account 書き込むアカウント
     * @param updatedBy 更新者のユーザーID
     * @param updatedAt 更新日時
     * @throws IllegalStateException 書き換える行が見つからない場合
     */
    @Override
    public void update(UserAccount account, UserId updatedBy, Instant updatedAt) {
        int updatedRows = dsl.update(CREDENTIALS)
                .set(CREDENTIALS.FAILED_LOGIN_COUNT, (byte) account.loginLock().failedCount())
                .set(
                        CREDENTIALS.LOCKED_AT,
                        toLocalDateTime(account.loginLock().lockedAt().orElse(null)))
                .set(
                        CREDENTIALS.UNLOCKED_AT,
                        toLocalDateTime(account.loginLock().unlockedAt().orElse(null)))
                .set(
                        CREDENTIALS.UNLOCKED_BY,
                        account.loginLock().unlockedBy().map(UserId::value).orElse(null))
                .set(CREDENTIALS.UPDATED_AT, toLocalDateTime(updatedAt))
                .set(CREDENTIALS.UPDATED_BY, updatedBy.value())
                .where(CREDENTIALS.USER_ID.eq(account.userId().value()))
                .execute(); // 実行して書き換えた行の数を返す
        if (updatedRows == 0) {
            throw new IllegalStateException("更新対象のアカウントが見つかりません。");
        }
    }

    /**
     * DBの1行をアカウントに変換
     *
     * @param row auth.credentialsの1行
     * @return アカウント
     */
    private UserAccount toUserAccount(CredentialsRecord row) { // DBの型（String・LocalDateTimeなど）からドメインの型に作り直す
        LoginLock loginLock = LoginLock.restore(
                row.getFailedLoginCount(), // 列はByte型だがint型の引数にそのまま渡せる（Javaが自動で変換する）
                toInstant(row.getLockedAt()), // 日本時間のLocalDateTimeをInstantに変換する
                toInstant(row.getUnlockedAt()),
                row.getUnlockedBy() == null ? null : new UserId(row.getUnlockedBy())); // 値がない列はnullのまま渡す
        return UserAccount.restore(
                new UserId(row.getUserId()),
                new EmailAddress(row.getEmail()),
                new PasswordHash(row.getPasswordHash()),
                loginLock);
    }

    /**
     * DBの日時（日本時間）をInstantに変換
     *
     * @param value DBの日時（日本時間）
     * @return 同じ時点のInstant（値がない場合はnull）
     */
    private static Instant toInstant(LocalDateTime value) {
        return value == null
                ? null
                : value.atZone(BusinessTimeZone.JAPAN).toInstant(); // 「日本の時計の時刻」としてタイムゾーンを付けてからInstantにする（9時→UTCの0時）
    }

    /**
     * InstantをDBの日時（日本時間）に変換
     *
     * @param value 日時
     * @return 同じ時点の日本時間の日時（値がない場合はnull）
     */
    private static LocalDateTime toLocalDateTime(Instant value) {
        return value == null
                ? null
                : LocalDateTime.ofInstant(value, BusinessTimeZone.JAPAN); // toInstantの逆（UTCの0時→日本時間の9時）
    }
}
