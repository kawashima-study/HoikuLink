package io.github.kawashima_study.hoikulink.shared;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * エラーコードとHTTPのステータスの対応表。各モジュールの入口（adapter.in.web）で、部品として登録する。
 * Springが部品に代理（子クラス）を付けることがあるため、finalにせず、コンストラクタもprotectedにする。
 */
public class ErrorStatusMapping {

    private final Map<String, HttpStatus> statuses;

    protected ErrorStatusMapping(Map<String, HttpStatus> statuses) {
        this.statuses = statuses;
    }

    /**
     * enumのすべてのエラーコードにステータスが決まっていることを確かめて、対応表を作る。
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

    public Map<String, HttpStatus> statuses() {
        return statuses;
    }
}
