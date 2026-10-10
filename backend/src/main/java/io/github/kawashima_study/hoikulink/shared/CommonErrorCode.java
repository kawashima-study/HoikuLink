package io.github.kawashima_study.hoikulink.shared;

/**
 * すべてのモジュールで使う共通のエラーコード。
 */
public enum CommonErrorCode implements ErrorCode {
    INVALID_REQUEST("リクエストの形式が正しくありません"),
    UNAUTHENTICATED("ログインが必要です"),
    FORBIDDEN("この操作を行う権限がありません"),
    RESOURCE_NOT_FOUND("対象が見つかりません"),
    VALIDATION_ERROR("入力内容に誤りがあります"),
    TOO_MANY_REQUESTS("しばらく時間をおいてから、もう一度お試しください"),
    INTERNAL_ERROR("システムエラーが発生しました"),
    SERVICE_UNAVAILABLE("ただいま利用できません");

    private final String title;

    CommonErrorCode(String title) {
        this.title = title;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public String title() {
        return title;
    }
}
