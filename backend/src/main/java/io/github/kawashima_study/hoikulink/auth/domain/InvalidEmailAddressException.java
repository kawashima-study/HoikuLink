package io.github.kawashima_study.hoikulink.auth.domain;

import io.github.kawashima_study.hoikulink.shared.ApplicationException;

/**
 * メールアドレスの長さまたは形式が正しくないことを表す例外
 */
public class InvalidEmailAddressException extends ApplicationException {

    /**
     * 例外を作成
     * <p>詳細には利用者が入力を直すための説明を入れる
     * <p>入力された値は個人の情報のため詳細に含めない
     */
    public InvalidEmailAddressException() {
        super(AuthErrorCode.INVALID_EMAIL_ADDRESS, "254文字以内の正しい形式のメールアドレスを入力してください。");
    }
}
