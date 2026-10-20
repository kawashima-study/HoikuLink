package io.github.kawashima_study.hoikulink.auth.adapter.in.web;

import io.github.kawashima_study.hoikulink.shared.ApplicationException;
import io.github.kawashima_study.hoikulink.shared.CommonErrorCode;

/**
 * リフレッシュトークンで再発行できない（401）
 * <p>理由（トークンがない・有効期限切れ・無効化済み）は画面に伝えない
 */
class RefreshTokenRejectedException extends ApplicationException {

    /**
     * 例外を作成
     */
    RefreshTokenRejectedException() {
        super(CommonErrorCode.UNAUTHENTICATED, "もう一度ログインしてください。");
    }
}
