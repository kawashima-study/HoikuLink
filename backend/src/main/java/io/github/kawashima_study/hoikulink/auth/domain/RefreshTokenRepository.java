package io.github.kawashima_study.hoikulink.auth.domain;

import io.github.kawashima_study.hoikulink.shared.UserId;
import java.time.Instant;

/**
 * リフレッシュトークンの保存のインターフェース
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
    void save(RefreshToken refreshToken, UserId createdBy, Instant createdAt); // テストで日時を固定しやすくするため作成者と作成日時は呼び出す側が渡す
}
