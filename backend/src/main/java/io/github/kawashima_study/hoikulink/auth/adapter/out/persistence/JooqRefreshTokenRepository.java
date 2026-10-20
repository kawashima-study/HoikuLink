package io.github.kawashima_study.hoikulink.auth.adapter.out.persistence;

import static io.github.kawashima_study.hoikulink.auth.adapter.out.jooq.generated.Tables.REFRESH_TOKENS;

import io.github.kawashima_study.hoikulink.auth.domain.RefreshToken;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshTokenRepository;
import io.github.kawashima_study.hoikulink.shared.BusinessTimeZone;
import io.github.kawashima_study.hoikulink.shared.UserId;
import java.time.Instant;
import java.time.LocalDateTime;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/**
 * リフレッシュトークンの保存のjOOQでの実装
 * <p>テーブルはauth.refresh_tokensを使う
 */
@Repository
class JooqRefreshTokenRepository implements RefreshTokenRepository {

    /** SQLを組み立てて実行するjOOQの入口 */
    private final DSLContext dsl;

    /**
     * Repositoryを作成
     *
     * @param dsl SQLを組み立てて実行するjOOQの入口
     */
    JooqRefreshTokenRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * リフレッシュトークンを新しく登録
     *
     * @param refreshToken 登録するリフレッシュトークン
     * @param createdBy 作成者のユーザーID
     * @param createdAt 作成日時
     */
    @Override
    public void save(RefreshToken refreshToken, UserId createdBy, Instant createdAt) {
        LocalDateTime createdAtInJapan = toLocalDateTime(createdAt);
        dsl.insertInto(REFRESH_TOKENS)
                .set(
                        REFRESH_TOKENS.REFRESH_TOKEN_ID,
                        refreshToken.refreshTokenId().value())
                .set(REFRESH_TOKENS.USER_ID, refreshToken.userId().value())
                .set(REFRESH_TOKENS.TOKEN_HASH, refreshToken.tokenHash().value()) // 生のトークンは保存しない
                .set(REFRESH_TOKENS.FAMILY_ID, refreshToken.familyId().value())
                .set(REFRESH_TOKENS.EXPIRES_AT, toLocalDateTime(refreshToken.expiresAt()))
                .set(
                        REFRESH_TOKENS.REVOKED_AT,
                        refreshToken.revokedAt().map(this::toLocalDateTime).orElse(null))
                .set(REFRESH_TOKENS.CREATED_AT, createdAtInJapan)
                .set(REFRESH_TOKENS.CREATED_BY, createdBy.value())
                .set(REFRESH_TOKENS.UPDATED_AT, createdAtInJapan) // 新しく登録するときは更新日時と更新者も作成時と同じにする
                .set(REFRESH_TOKENS.UPDATED_BY, createdBy.value())
                .execute();
    }

    /**
     * InstantをDBのDATETIME（日本時間）の値に変換
     *
     * @param value 変換する日時
     * @return 日本時間の日時
     */
    private LocalDateTime toLocalDateTime(Instant value) {
        return LocalDateTime.ofInstant(value, BusinessTimeZone.JAPAN);
    }
}
