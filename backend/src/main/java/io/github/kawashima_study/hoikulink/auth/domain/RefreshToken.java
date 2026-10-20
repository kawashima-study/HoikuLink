package io.github.kawashima_study.hoikulink.auth.domain;

import io.github.kawashima_study.hoikulink.shared.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * リフレッシュトークン（集約ルート）
 * <p>DBに保存する内容（ハッシュ値・トークンファミリー・有効期限）を持ち生のトークンは持たない
 * <p>交換（ローテーション）と無効化のルールを持つ
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

    /** 有効期限（交換しても延長しない） */
    private final Instant expiresAt;

    /** 無効化日時（無効化されていない場合はnull） */
    private Instant revokedAt; // 無効化で値が変わるためfinalにしない

    /**
     * 保存されている値からリフレッシュトークンを作成
     * <p>新しく発行する場合はissueを使う
     *
     * @param refreshTokenId リフレッシュトークンID
     * @param userId トークンを発行したアカウントのユーザーID
     * @param tokenHash 生のリフレッシュトークンのハッシュ値
     * @param familyId トークンファミリーID
     * @param expiresAt 有効期限
     * @param revokedAt 無効化日時（無効化されていない場合はnull）
     * @throws NullPointerException 無効化日時以外の引数がnullの場合
     */
    public RefreshToken(
            RefreshTokenId refreshTokenId,
            UserId userId,
            RefreshTokenHash tokenHash,
            RefreshTokenId familyId,
            Instant expiresAt,
            Instant revokedAt) {
        this.refreshTokenId = Objects.requireNonNull(refreshTokenId, "リフレッシュトークンIDがnullです。");
        this.userId = Objects.requireNonNull(userId, "ユーザーIDがnullです。");
        this.tokenHash = Objects.requireNonNull(tokenHash, "ハッシュ値がnullです。");
        this.familyId = Objects.requireNonNull(familyId, "トークンファミリーIDがnullです。");
        this.expiresAt = Objects.requireNonNull(expiresAt, "有効期限がnullです。");
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
     * 新しいリフレッシュトークンと交換する
     * <p>このトークンを無効化し同じトークンファミリーと同じ有効期限の新しいトークンを返す
     *
     * @param newTokenHash 新しい生のリフレッシュトークンのハッシュ値
     * @param rotatedAt 交換日時
     * @return 新しいリフレッシュトークン
     * @throws IllegalStateException 無効化済みまたは有効期限切れの場合
     * @throws NullPointerException いずれかの引数がnullの場合
     */
    public RefreshToken rotate(RefreshTokenHash newTokenHash, Instant rotatedAt) {
        Objects.requireNonNull(rotatedAt, "交換日時がnullです。");
        if (!isUsable(rotatedAt)) { // 使えないトークンの交換はプログラムの誤り（呼び出す側が先にisUsableで確認する）
            throw new IllegalStateException("使えないリフレッシュトークンは交換できません。");
        }
        this.revokedAt = rotatedAt;
        return new RefreshToken(
                RefreshTokenId.newId(),
                userId,
                newTokenHash,
                familyId, // 同じログインのグループを引き継ぐ
                expiresAt, // 交換しても有効期限は延長しない
                null);
    }

    /**
     * 使えるトークンかを返す
     *
     * @param now 現在の時刻
     * @return 無効化されておらず有効期限前の場合はtrue
     */
    public boolean isUsable(Instant now) {
        return !isRevoked() && now.isBefore(expiresAt);
    }

    /**
     * 無効化済みかを返す
     * <p>無効化済みのトークンが使われた場合は盗まれた可能性があるため同じトークンファミリーをすべて無効化する
     *
     * @return 無効化済みの場合はtrue
     */
    public boolean isRevoked() {
        return revokedAt != null;
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
        return Optional.ofNullable(revokedAt);
    }
}
