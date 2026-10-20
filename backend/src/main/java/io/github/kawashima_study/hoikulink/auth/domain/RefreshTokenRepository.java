package io.github.kawashima_study.hoikulink.auth.domain;

import io.github.kawashima_study.hoikulink.shared.UserId;
import java.time.Instant;
import java.util.Optional;

/**
 * リフレッシュトークンの保存と読み出しのインターフェース
 * <p>DBの読み書きの方法（jOOQ）は知らずメソッドの形だけを決める
 * <p>実装はadapterの中に置く
 */
public interface RefreshTokenRepository {

    /**
     * リフレッシュトークンを新しく登録
     * <p>作成者と更新者に作成者を入れ作成日時と更新日時に作成日時を入れる
     *
     * @param refreshToken 登録するリフレッシュトークン
     * @param createdBy 作成者のユーザーID
     * @param createdAt 作成日時
     */
    void save(RefreshToken refreshToken, UserId createdBy, Instant createdAt);

    /**
     * ハッシュ値でリフレッシュトークンを探しその行をロックする
     * <p>同じトークンでの再発行が同時に来た場合に後の処理を待たせる
     *
     * @param tokenHash 生のリフレッシュトークンのハッシュ値
     * @return 見つかったリフレッシュトークン（見つからない場合は空）
     */
    Optional<RefreshToken> findByHashForUpdate(RefreshTokenHash tokenHash);

    /**
     * リフレッシュトークンの無効化日時を書き込む
     *
     * @param refreshToken 書き込むリフレッシュトークン
     * @param updatedBy 更新者のユーザーID
     * @param updatedAt 更新日時
     * @throws IllegalStateException 書き換える行が見つからない場合
     */
    void update(RefreshToken refreshToken, UserId updatedBy, Instant updatedAt);

    /**
     * 同じトークンファミリーのまだ無効化されていないリフレッシュトークンをすべて無効化する
     * <p>ログアウトと使い回しの検知で使う
     *
     * @param familyId トークンファミリーID
     * @param updatedBy 更新者のユーザーID
     * @param revokedAt 無効化日時（更新日時にも入れる）
     */
    void revokeFamily(RefreshTokenId familyId, UserId updatedBy, Instant revokedAt);
}
