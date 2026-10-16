package io.github.kawashima_study.hoikulink.shared;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * エラーコードとHTTPのステータスの対応表
 * <p>各モジュールの入口（adapter.in.web）でSpringの部品として登録する
 * <p>Springが部品に代理（子クラス）を付けることがあるためfinalにせずコンストラクタもprotectedにする
 */
public class ErrorStatusMapping {

    /** エラーコードとHTTPのステータスの対応 */
    private final Map<String, HttpStatus> statuses;

    /**
     * 対応表を作成
     * <p>外部からは of を使用する
     *
     * @param statuses エラーコードとHTTPのステータスの対応
     */
    protected ErrorStatusMapping(Map<String, HttpStatus> statuses) {
        this.statuses = statuses;
    }

    /**
     * enumのすべてのエラーコードにステータスが決まっていることを確認して対応表を作成
     *
     * @param <E> ErrorCode を実装した enum の型
     * @param type エラーコードの enum の型
     * @param statuses エラーコードとHTTPのステータスの対応
     * @return 対応表
     * @throws IllegalStateException ステータスが決まっていないエラーコードがある場合
     */
    public static <E extends Enum<E> & ErrorCode> ErrorStatusMapping of(Class<E> type, Map<E, HttpStatus> statuses) {
        Map<String, HttpStatus> byCode = new HashMap<>();
        for (E errorCode : type.getEnumConstants()) {
            HttpStatus status = statuses.get(errorCode);
            if (status == null) {
                throw new IllegalStateException("HTTPのステータスが決まっていないエラーコードがあります：" + errorCode.code());
            }
            byCode.put(errorCode.code(), status);
        }
        return new ErrorStatusMapping(Map.copyOf(byCode));
    }

    /**
     * エラーコードとHTTPのステータスの対応を返す
     *
     * @return 変更できない対応表
     */
    public Map<String, HttpStatus> statuses() {
        return statuses;
    }
}
