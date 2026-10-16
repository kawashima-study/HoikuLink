package io.github.kawashima_study.hoikulink.shared;

import java.util.Objects;

/**
 * 業務のエラーを表す例外の土台
 * <p>メッセージはそのまま画面に表示してよい文のみにする
 */
public class ApplicationException extends RuntimeException {

    /** エラーの種類を表すエラーコード */
    private final transient ErrorCode errorCode;

    /**
     * 例外を作成
     *
     * @param errorCode エラーの種類を表すエラーコード
     * @param detail 画面に表示する詳細の説明
     * @throws NullPointerException エラーコードが null の場合
     */
    public ApplicationException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = Objects.requireNonNull(errorCode, "エラーコードがnullです。");
    }

    /**
     * エラーコードを返す
     *
     * @return エラーの種類を表すエラーコード
     */
    public ErrorCode errorCode() {
        return errorCode;
    }
}
