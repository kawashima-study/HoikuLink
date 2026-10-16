package io.github.kawashima_study.hoikulink.shared;

/**
 * すべてのモジュールで使用する共通のエラーコード
 */
public enum CommonErrorCode implements ErrorCode {

    /** リクエストの形式の誤り */
    INVALID_REQUEST("リクエストの形式が正しくありません。"),

    /** ログインしていない */
    UNAUTHENTICATED("ログインが必要です。"),

    /** 操作の権限がない */
    FORBIDDEN("この操作を行う権限がありません。"),

    /** 対象が存在しないまたは閲覧できる範囲の外にある */
    RESOURCE_NOT_FOUND("対象が見つかりません。"),

    /** 入力チェックの誤り */
    VALIDATION_ERROR("入力内容に誤りがあります。"),

    /** リクエストの回数が多すぎる */
    TOO_MANY_REQUESTS("しばらく時間をおいてからもう一度お試しください。"),

    /** 想定外のエラー */
    INTERNAL_ERROR("システムエラーが発生しました。"),

    /** 一時的に利用できない */
    SERVICE_UNAVAILABLE("ただいま利用できません。");

    /** 画面に表示できるエラーの種類の短い説明 */
    private final String title;

    /**
     * エラーコードを作成
     *
     * @param title 画面に表示できるエラーの種類の短い説明
     */
    CommonErrorCode(String title) {
        this.title = title;
    }

    /**
     * 画面が処理を分けるのに使うコードを返す
     *
     * @return エラーコードの名前（例 RESOURCE_NOT_FOUND）
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
