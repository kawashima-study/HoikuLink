package io.github.kawashima_study.hoikulink.auth.adapter.out.persistence;

import static io.github.kawashima_study.hoikulink.auth.adapter.out.jooq.generated.Tables.REFRESH_TOKENS;

import io.github.kawashima_study.hoikulink.auth.adapter.out.jooq.generated.tables.records.RefreshTokensRecord;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshToken;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshTokenHash;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshTokenId;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshTokenRepository;
import io.github.kawashima_study.hoikulink.shared.BusinessTimeZone;
import io.github.kawashima_study.hoikulink.shared.UserId;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/**
 * リフレッシュトークンの保存と読み出しのjOOQでの実装
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
     * ハッシュ値でリフレッシュトークンを探しその行をロックする
     *
     * @param tokenHash 生のリフレッシュトークンのハッシュ値
     * @return 見つかったリフレッシュトークン（見つからない場合は空）
     */
    @Override
    public Optional<RefreshToken> findByHashForUpdate(RefreshTokenHash tokenHash) {
        return dsl.selectFrom(REFRESH_TOKENS)
                .where(REFRESH_TOKENS.TOKEN_HASH.eq(tokenHash.value()))
                .forUpdate() // 同じトークンでの再発行が同時に来たら後の処理を待たせる
                .fetchOptional()
                .map(this::toRefreshToken);
    }

    /**
     * リフレッシュトークンの無効化日時を書き込む
     *
     * @param refreshToken 書き込むリフレッシュトークン
     * @param updatedBy 更新者のユーザーID
     * @param updatedAt 更新日時
     * @throws IllegalStateException 書き換える行が見つからない場合
     */
    @Override
    public void update(RefreshToken refreshToken, UserId updatedBy, Instant updatedAt) {
        int updated = dsl.update(REFRESH_TOKENS)
                .set(
                        REFRESH_TOKENS.REVOKED_AT,
                        refreshToken.revokedAt().map(this::toLocalDateTime).orElse(null))
                .set(REFRESH_TOKENS.UPDATED_AT, toLocalDateTime(updatedAt))
                .set(REFRESH_TOKENS.UPDATED_BY, updatedBy.value())
                .where(REFRESH_TOKENS.REFRESH_TOKEN_ID.eq(
                        refreshToken.refreshTokenId().value()))
                .execute(); // 書き換えた行の数を返す
        if (updated == 0) {
            throw new IllegalStateException("更新対象のリフレッシュトークンが見つかりません。");
        }
    }

    /**
     * 同じトークンファミリーのまだ無効化されていないリフレッシュトークンをすべて無効化する
     *
     * @param familyId トークンファミリーID
     * @param updatedBy 更新者のユーザーID
     * @param revokedAt 無効化日時（更新日時にも入れる）
     */
    @Override
    public void revokeFamily(RefreshTokenId familyId, UserId updatedBy, Instant revokedAt) {
        LocalDateTime revokedAtInJapan = toLocalDateTime(revokedAt);
        dsl.update(REFRESH_TOKENS)
                .set(REFRESH_TOKENS.REVOKED_AT, revokedAtInJapan)
                .set(REFRESH_TOKENS.UPDATED_AT, revokedAtInJapan)
                .set(REFRESH_TOKENS.UPDATED_BY, updatedBy.value())
                .where(REFRESH_TOKENS.FAMILY_ID.eq(familyId.value()))
                .and(REFRESH_TOKENS.REVOKED_AT.isNull()) // 無効化済みの行の無効化日時は変えない
                .execute();
    }

    /**
     * DBの行をリフレッシュトークンに変換
     *
     * @param row auth.refresh_tokensの行
     * @return リフレッシュトークン
     */
    private RefreshToken toRefreshToken(RefreshTokensRecord row) {
        return new RefreshToken(
                new RefreshTokenId(row.getRefreshTokenId()),
                new UserId(row.getUserId()),
                new RefreshTokenHash(row.getTokenHash()),
                new RefreshTokenId(row.getFamilyId()),
                toInstant(row.getExpiresAt()),
                row.getRevokedAt() == null ? null : toInstant(row.getRevokedAt()));
    }

    /**
     * DBのDATETIME（日本時間）の値をInstantに変換
     *
     * @param value 日本時間の日時
     * @return 変換した日時
     */
    private Instant toInstant(LocalDateTime value) {
        return value.atZone(BusinessTimeZone.JAPAN).toInstant();
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
