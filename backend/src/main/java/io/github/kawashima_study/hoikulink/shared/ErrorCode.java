package io.github.kawashima_study.hoikulink.shared;

/**
 * エラーコードの共通の形
 * <p>HTTPのステータスは持たない（各モジュールの入口で決める）
 */
public interface ErrorCode {

    /**
     * 画面が処理を分けるのに使うコードを返す
     *
     * @return エラーコード（例 RESOURCE_NOT_FOUND）
     */
    String code();

    /**
     * エラーの種類の短い説明を返す
     *
     * @return 画面に表示できるエラーの種類の短い説明
     */
    String title();
}
