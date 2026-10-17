package io.github.kawashima_study.hoikulink.auth.domain;

import io.github.kawashima_study.hoikulink.shared.UserId;
import java.time.Instant;
import java.util.Optional;

/**
 * アカウントの保存と読み出しのインターフェース
 * <p>DBの読み書きの方法（jOOQ）は知らず メソッドの形だけを決める
 * <p>実装はadapterの中に置く
 */
public interface UserAccountRepository {

    /**
     * メールアドレスでアカウントを探しその行をロックする
     * <p>削除済みの行は対象外とする
     * <p>ロックはトランザクションの終わりまで続くためトランザクションの中で呼ぶ
     *
     * @param emailAddress ログインID（メールアドレス）
     * @return 見つかったアカウント（見つからない場合は空）
     */
    Optional<UserAccount> findByEmailForUpdate(EmailAddress emailAddress); // 対象レコードが存在しない可能性のあるデータのためOptionalで定義する

    /**
     * ユーザーIDでアカウントを探しその行をロックする
     * <p>削除済みの行は対象外とする
     * <p>ロックはトランザクションの終わりまで続くためトランザクションの中で呼ぶ
     *
     * @param userId ユーザーID
     * @return 見つかったアカウント（見つからない場合は空）
     */
    Optional<UserAccount> findByIdForUpdate(UserId userId);

    /**
     * アカウントのログインの失敗回数とロックの状態を書き込む
     * <p>失敗回数 ロック日時 解除日時 解除者 更新日時 更新者を書き換える
     *
     * @param account 書き込むアカウント
     * @param updatedBy 更新者のユーザーID
     * @param updatedAt 更新日時
     * @throws IllegalStateException 書き換える行が見つからない場合
     */
    void update(UserAccount account, UserId updatedBy, Instant updatedAt); // テストで日時を固定しやすくするため更新者と更新日時は呼び出す側が渡す
}
