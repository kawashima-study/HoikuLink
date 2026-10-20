package io.github.kawashima_study.hoikulink.auth.adapter.in.web;

import io.github.kawashima_study.hoikulink.shared.ApplicationException;
import io.github.kawashima_study.hoikulink.shared.CommonErrorCode;

/**
 * CSRF対策の確認に通らなかったリクエスト（403）
 */
class CsrfCheckFailedException extends ApplicationException {

    /**
     * 例外を作成
     */
    CsrfCheckFailedException() {
        super(CommonErrorCode.FORBIDDEN, "HoikuLinkの画面から操作してください。");
    }
}
