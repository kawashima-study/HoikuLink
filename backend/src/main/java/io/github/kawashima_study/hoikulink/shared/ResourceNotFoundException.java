package io.github.kawashima_study.hoikulink.shared;

/**
 * 対象が存在しない、または閲覧できる範囲の外にあることを表す。
 */
public class ResourceNotFoundException extends ApplicationException {

    public ResourceNotFoundException(String detail) {
        super(CommonErrorCode.RESOURCE_NOT_FOUND, detail);
    }
}
