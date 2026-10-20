package io.github.kawashima_study.hoikulink.auth.adapter.out.persistence;

import static io.github.kawashima_study.hoikulink.auth.adapter.out.jooq.generated.Tables.REFRESH_TOKENS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.kawashima_study.hoikulink.TestcontainersConfiguration;
import io.github.kawashima_study.hoikulink.auth.adapter.out.jooq.generated.tables.records.RefreshTokensRecord;
import io.github.kawashima_study.hoikulink.auth.domain.RawRefreshToken;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshToken;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshTokenHash;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshTokenRepository;
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
 * リフレッシュトークンのRepository（JooqRefreshTokenRepository）のテスト
 * <p>Testcontainersで起動した本物のMySQLに読み書きして確認する
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional // テストごとにトランザクションを始め 終わったら取り消す
@DisplayName("リフレッシュトークンのRepository")
class JooqRefreshTokenRepositoryTests {

    /** 発行日時と作成日時（テストデータ） */
    private static final Instant NOW = Instant.parse("2026-10-20T01:00:00Z");

    /** NOWを日本時間で表した日時（UTCの1時は日本時間の10時） */
    private static final LocalDateTime NOW_IN_JAPAN = LocalDateTime.of(2026, 10, 20, 10, 0);

    /** 有効期限を日本時間で表した日時（発行の12時間後） */
    private static final LocalDateTime EXPIRES_AT_IN_JAPAN = LocalDateTime.of(2026, 10, 20, 22, 0);

    /** 交換日時（テストデータ） */
    private static final Instant ROTATED_AT = Instant.parse("2026-10-20T02:00:00Z");

    /** ROTATED_ATを日本時間で表した日時 */
    private static final LocalDateTime ROTATED_AT_IN_JAPAN = LocalDateTime.of(2026, 10, 20, 11, 0);

    /** トークンファミリーを無効化した日時（テストデータ） */
    private static final Instant REVOKED_AT = Instant.parse("2026-10-20T03:00:00Z");

    /** REVOKED_ATを日本時間で表した日時 */
    private static final LocalDateTime REVOKED_AT_IN_JAPAN = LocalDateTime.of(2026, 10, 20, 12, 0);

    /** テスト対象のRepository */
    @Autowired
    private RefreshTokenRepository repository;

    /** 書き込んだ行を直接読むためのjOOQの入口 */
    @Autowired
    private DSLContext dsl;

    /**
     * リフレッシュトークンの内容をDBに登録することを確認
     */
    @Test
    @DisplayName("リフレッシュトークンの内容をDBに登録する。")
    void savesRefreshToken() {
        // リフレッシュトークンを発行して登録
        UserId userId = UserId.newId();
        RefreshTokenHash tokenHash = hashOf("test-refresh-token");
        RefreshToken token = RefreshToken.issue(userId, tokenHash, NOW);
        repository.save(token, userId, NOW);

        // 登録した行の内容を確認
        RefreshTokensRecord row = findRow(token);
        assertThat(row.getUserId()).isEqualTo(userId.value());
        assertThat(row.getTokenHash()).isEqualTo(tokenHash.value());
        assertThat(row.getFamilyId()).isEqualTo(token.familyId().value());
        assertThat(row.getExpiresAt()).isEqualTo(EXPIRES_AT_IN_JAPAN); // InstantのUTCが日本時間に変換されていることを確認
        assertThat(row.getRevokedAt()).isNull();
    }

    /**
     * 作成者と更新者に作成者を入れ作成日時と更新日時に作成日時を入れることを確認
     */
    @Test
    @DisplayName("作成者と更新者に作成者を入れ作成日時と更新日時に作成日時を入れる。")
    void savesAuditColumns() {
        // リフレッシュトークンを発行して登録
        UserId userId = UserId.newId();
        RefreshToken token = RefreshToken.issue(userId, hashOf("test-refresh-token"), NOW);
        repository.save(token, userId, NOW);

        // 監査項目を確認
        RefreshTokensRecord row = findRow(token);
        assertThat(row.getCreatedBy()).isEqualTo(userId.value());
        assertThat(row.getUpdatedBy()).isEqualTo(userId.value());
        assertThat(row.getCreatedAt()).isEqualTo(NOW_IN_JAPAN);
        assertThat(row.getUpdatedAt()).isEqualTo(NOW_IN_JAPAN);
    }

    /**
     * ハッシュ値で登録したリフレッシュトークンを読み込めることを確認
     */
    @Test
    @DisplayName("ハッシュ値で登録したリフレッシュトークンを読み込める。")
    void findsByHash() {
        // リフレッシュトークンを登録
        UserId userId = UserId.newId();
        RefreshTokenHash tokenHash = hashOf("test-refresh-token");
        RefreshToken token = RefreshToken.issue(userId, tokenHash, NOW);
        repository.save(token, userId, NOW);

        // ハッシュ値で読み込む
        Optional<RefreshToken> found = repository.findByHashForUpdate(tokenHash);

        // 登録した内容で読み込めたことを確認
        assertThat(found).isPresent();
        assertThat(found.get().refreshTokenId()).isEqualTo(token.refreshTokenId());
        assertThat(found.get().userId()).isEqualTo(userId);
        assertThat(found.get().expiresAt()).isEqualTo(token.expiresAt());
        assertThat(found.get().isRevoked()).isFalse();
    }

    /**
     * 登録されていないハッシュ値では何も見つからないことを確認
     */
    @Test
    @DisplayName("登録されていないハッシュ値では何も見つからない。")
    void findsNothingForUnknownHash() {
        // 登録していないハッシュ値で読み込むと空であることを確認
        assertThat(repository.findByHashForUpdate(hashOf("unknown-refresh-token")))
                .isEmpty();
    }

    /**
     * 交換で無効化した日時を書き込むことを確認
     */
    @Test
    @DisplayName("交換で無効化した日時を書き込む。")
    void updatesRevokedAt() {
        // 登録したトークンを交換して元のトークンを無効化
        UserId userId = UserId.newId();
        RefreshToken token = RefreshToken.issue(userId, hashOf("test-refresh-token"), NOW);
        repository.save(token, userId, NOW);
        token.rotate(hashOf("new-refresh-token"), ROTATED_AT);

        // 無効化を書き込む
        repository.update(token, userId, ROTATED_AT);

        // 無効化日時と更新日時が交換日時になっていることを確認
        RefreshTokensRecord row = findRow(token);
        assertThat(row.getRevokedAt()).isEqualTo(ROTATED_AT_IN_JAPAN);
        assertThat(row.getUpdatedAt()).isEqualTo(ROTATED_AT_IN_JAPAN);
    }

    /**
     * DBにないトークンを書き込もうとすると例外になることを確認
     */
    @Test
    @DisplayName("DBにないトークンを書き込もうとすると例外になる。")
    void rejectsUpdateOfMissingToken() {
        // 登録していないトークンを用意
        UserId userId = UserId.newId();
        RefreshToken token = RefreshToken.issue(userId, hashOf("test-refresh-token"), NOW);

        // 書き込むと例外になることを確認
        assertThatThrownBy(() -> repository.update(token, userId, NOW)).isInstanceOf(IllegalStateException.class);
    }

    /**
     * 同じトークンファミリーのまだ無効化されていないトークンをすべて無効化することを確認
     */
    @Test
    @DisplayName("同じトークンファミリーのまだ無効化されていないトークンをすべて無効化する。")
    void revokesFamily() {
        // 交換して2つになったトークンを登録（元のトークンは交換日時で無効化済み）
        UserId userId = UserId.newId();
        RefreshToken first = RefreshToken.issue(userId, hashOf("first-refresh-token"), NOW);
        repository.save(first, userId, NOW);
        RefreshToken second = first.rotate(hashOf("second-refresh-token"), ROTATED_AT);
        repository.update(first, userId, ROTATED_AT);
        repository.save(second, userId, ROTATED_AT);

        // トークンファミリーを無効化
        repository.revokeFamily(first.familyId(), userId, REVOKED_AT);

        // 新しいトークンは無効化され元のトークンの無効化日時は変わらないことを確認
        assertThat(findRow(second).getRevokedAt()).isEqualTo(REVOKED_AT_IN_JAPAN);
        assertThat(findRow(first).getRevokedAt()).isEqualTo(ROTATED_AT_IN_JAPAN);
    }

    /**
     * 登録した行をIDで読み込む
     *
     * @param token 登録したリフレッシュトークン
     * @return 登録した行
     */
    private RefreshTokensRecord findRow(RefreshToken token) {
        return dsl.selectFrom(REFRESH_TOKENS)
                .where(REFRESH_TOKENS.REFRESH_TOKEN_ID.eq(token.refreshTokenId().value()))
                .fetchSingle();
    }

    /**
     * 生のリフレッシュトークンのハッシュ値を計算
     *
     * @param rawRefreshToken 生のリフレッシュトークン
     * @return ハッシュ値
     */
    private static RefreshTokenHash hashOf(String rawRefreshToken) {
        return RefreshTokenHash.of(new RawRefreshToken(rawRefreshToken));
    }
}
