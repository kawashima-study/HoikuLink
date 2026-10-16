package io.github.kawashima_study.hoikulink.auth.domain;

import io.github.kawashima_study.hoikulink.shared.ErrorCode;

/**
 * 認証モジュールのエラーコード
 */
public enum AuthErrorCode implements ErrorCode {

    /** メールアドレスの長さまたは形式の誤り */
    INVALID_EMAIL_ADDRESS("メールアドレスの形式が正しくありません。"),

    /** ロック解除を自分自身で行う */
    SELF_UNLOCK_NOT_ALLOWED("自分のアカウントのロックは解除できません。");

    /** 画面に表示できるエラーの種類の短い説明 */
    private final String title;

    /**
     * エラーコードを作成
     *
     * @param title 画面に表示できるエラーの種類の短い説明
     */
    AuthErrorCode(String title) {
        this.title = title;
    }

    /**
     * 画面が処理を分けるのに使うコードを返す
     *
     * @return エラーコードの名前（例 INVALID_EMAIL_ADDRESS）
     */
    @Override
    public String code() {
        return name();
    }

    /**
     * エラーの種類の短い説明を返す
     *
     * @return 画面に表示できるエラーの種類の短い説明
     */
    @Override
    public String title() {
        return title;
    }
}
