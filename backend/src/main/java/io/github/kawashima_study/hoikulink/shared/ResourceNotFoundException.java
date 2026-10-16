package io.github.kawashima_study.hoikulink.shared;

/**
 * 対象が存在しないまたは閲覧できる範囲の外にあることを表す例外
 * <p>権限がない場合も存在しない場合と区別せず404とする（対象が存在することを知られないため）
 */
public class ResourceNotFoundException extends ApplicationException {

    /**
     * 例外を作成
     *
     * @param detail 画面に表示する詳細の説明
     */
    public ResourceNotFoundException(String detail) {
        super(CommonErrorCode.RESOURCE_NOT_FOUND, detail);
    }
}
