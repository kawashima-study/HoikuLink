package io.github.kawashima_study.hoikulink.shared;

import java.util.Objects;

/**
 * 業務のエラーを表す例外の土台。メッセージは、そのまま画面に表示してよい文だけにする。
 */
public class ApplicationException extends RuntimeException {

    private final transient ErrorCode errorCode;

    public ApplicationException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = Objects.requireNonNull(errorCode, "エラーコードがnullです。");
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
