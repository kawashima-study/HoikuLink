package io.github.kawashima_study.hoikulink.auth.domain;

import io.github.kawashima_study.hoikulink.shared.UserId;
import java.time.Instant;

/**
 * アクセストークンを発行するインターフェース
 * <p>アクセストークンの作り方（JWT）は知らずメソッドの形だけを決める
 * <p>実装はadapterの中に置く
 */
public interface AccessTokenIssuer {

    /**
     * ユーザーIDのアクセストークンを発行
     *
     * @param userId アクセストークンを発行するアカウントのユーザーID
     * @param issuedAt 発行日時
     * @return 発行したアクセストークン
     */
    AccessToken issue(UserId userId, Instant issuedAt); // テストで日時を固定しやすくするため発行日時は呼び出す側が渡す
}
