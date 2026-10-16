package io.github.kawashima_study.hoikulink.auth.domain;

import io.github.kawashima_study.hoikulink.shared.ApplicationException;

/**
 * 自分自身でアカウントのロック解除が不可であること表す例外
 */
public class SelfUnlockNotAllowedException extends ApplicationException {

    /**
     * 例外を作成
     * <p>詳細には利用者が次に取るべき行動を入れる
     */
    public SelfUnlockNotAllowedException() {
        super(AuthErrorCode.SELF_UNLOCK_NOT_ALLOWED, "ほかの園長または主任にロックの解除を依頼してください。");
    }
}
