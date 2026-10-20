package io.github.kawashima_study.hoikulink.auth.adapter.out.persistence;

import static io.github.kawashima_study.hoikulink.auth.adapter.out.jooq.generated.Tables.REFRESH_TOKENS;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.kawashima_study.hoikulink.TestcontainersConfiguration;
import io.github.kawashima_study.hoikulink.auth.adapter.out.jooq.generated.tables.records.RefreshTokensRecord;
import io.github.kawashima_study.hoikulink.auth.domain.RawRefreshToken;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshToken;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshTokenHash;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshTokenRepository;
import io.github.kawashima_study.hoikulink.shared.UserId;
import java.time.Instant;
import java.time.LocalDateTime;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * リフレッシュトークンのRepository（JooqRefreshTokenRepository）のテスト
 * <p>Testcontainersで起動した本物のMySQLに書き込んで確認する
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
        // リフレッシュトークンを発行
        UserId userId = UserId.newId();
        RefreshTokenHash tokenHash = RefreshTokenHash.of(new RawRefreshToken("test-refresh-token"));
        RefreshToken token = RefreshToken.issue(userId, tokenHash, NOW);

        // 登録
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
        RefreshToken token =
                RefreshToken.issue(userId, RefreshTokenHash.of(new RawRefreshToken("test-refresh-token")), NOW);
        repository.save(token, userId, NOW);

        // 監査項目を確認
        RefreshTokensRecord row = findRow(token);
        assertThat(row.getCreatedBy()).isEqualTo(userId.value());
        assertThat(row.getUpdatedBy()).isEqualTo(userId.value());
        assertThat(row.getCreatedAt()).isEqualTo(NOW_IN_JAPAN);
        assertThat(row.getUpdatedAt()).isEqualTo(NOW_IN_JAPAN);
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
                .fetchSingle(); // 1件でなければ例外にする
    }
}
