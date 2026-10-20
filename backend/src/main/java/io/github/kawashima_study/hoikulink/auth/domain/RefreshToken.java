package io.github.kawashima_study.hoikulink.auth.domain;

import io.github.kawashima_study.hoikulink.shared.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * リフレッシュトークン（集約ルート）
 * <p>DBに保存する内容（ハッシュ値・トークンファミリー・有効期限）を持ち生のトークンは持たない
 */
public final class RefreshToken {

    /** 有効期間（最初のログインから12時間） */
    private static final Duration VALIDITY = Duration.ofHours(12);

    /** リフレッシュトークンID */
    private final RefreshTokenId refreshTokenId;

    /** トークンを発行したアカウントのユーザーID */
    private final UserId userId;

    /** 生のリフレッシュトークンのハッシュ値 */
    private final RefreshTokenHash tokenHash;

    /** 1回のログインから交換で続いていくトークンのグループのID */
    private final RefreshTokenId familyId;

    /** 有効期限 */
    private final Instant expiresAt;

    /** 無効化日時（無効化されていない場合はnull） */
    private Instant revokedAt; // 今後の無効化で値が変わるためfinalにしない

    /**
     * リフレッシュトークンを作成
     * <p>外からはissueで作るためprivateにする
     *
     * @param refreshTokenId リフレッシュトークンID
     * @param userId トークンを発行したアカウントのユーザーID
     * @param tokenHash 生のリフレッシュトークンのハッシュ値
     * @param familyId トークンファミリーID
     * @param expiresAt 有効期限
     * @param revokedAt 無効化日時（無効化されていない場合はnull）
     */
    private RefreshToken(
            RefreshTokenId refreshTokenId,
            UserId userId,
            RefreshTokenHash tokenHash,
            RefreshTokenId familyId,
            Instant expiresAt,
            Instant revokedAt) {
        this.refreshTokenId = refreshTokenId;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
    }

    /**
     * ログインで新しいリフレッシュトークンを発行
     * <p>トークンファミリーIDは自分のIDとし有効期限は発行日時の12時間後とする
     *
     * @param userId トークンを発行するアカウントのユーザーID
     * @param tokenHash 生のリフレッシュトークンのハッシュ値
     * @param issuedAt 発行日時
     * @return 発行したリフレッシュトークン
     * @throws NullPointerException いずれかの引数がnullの場合
     */
    public static RefreshToken issue(UserId userId, RefreshTokenHash tokenHash, Instant issuedAt) {
        Objects.requireNonNull(userId, "ユーザーIDがnullです。");
        Objects.requireNonNull(tokenHash, "ハッシュ値がnullです。");
        Objects.requireNonNull(issuedAt, "発行日時がnullです。");
        RefreshTokenId refreshTokenId = RefreshTokenId.newId();
        return new RefreshToken(
                refreshTokenId,
                userId,
                tokenHash,
                refreshTokenId, // ログインで発行する最初のトークンはトークンファミリーIDを自分のIDにする
                issuedAt.plus(VALIDITY),
                null); // 発行したときは無効化されていない
    }

    /**
     * リフレッシュトークンIDを返す
     *
     * @return リフレッシュトークンID
     */
    public RefreshTokenId refreshTokenId() {
        return refreshTokenId;
    }

    /**
     * トークンを発行したアカウントのユーザーIDを返す
     *
     * @return ユーザーID
     */
    public UserId userId() {
        return userId;
    }

    /**
     * 生のリフレッシュトークンのハッシュ値を返す
     *
     * @return ハッシュ値
     */
    public RefreshTokenHash tokenHash() {
        return tokenHash;
    }

    /**
     * トークンファミリーIDを返す
     *
     * @return トークンファミリーID
     */
    public RefreshTokenId familyId() {
        return familyId;
    }

    /**
     * 有効期限を返す
     *
     * @return 有効期限
     */
    public Instant expiresAt() {
        return expiresAt;
    }

    /**
     * 無効化日時を返す
     *
     * @return 無効化日時（無効化されていない場合は空）
     */
    public Optional<Instant> revokedAt() {
        return Optional.ofNullable(revokedAt); // 値がない可能性があるためnullではなくOptionalで返す
    }
}
