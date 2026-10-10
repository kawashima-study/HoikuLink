package io.github.kawashima_study.hoikulink.shared;

/**
 * エラーコードの共通の形。HTTPのステータスは持たない（入口の層で決める）。
 */
public interface ErrorCode {

    /** 画面が処理を分けるのに使うコード（例：RESOURCE_NOT_FOUND）。 */
    String code();

    /** 画面に表示できる、エラーの種類の短い説明。 */
    String title();
}
