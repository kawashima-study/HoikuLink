package io.github.kawashima_study.hoikulink.shared.web;

import io.github.kawashima_study.hoikulink.shared.ErrorCode;
import io.github.kawashima_study.hoikulink.shared.ErrorStatusMapping;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 各モジュールが登録した対応表をまとめ、エラーコードからHTTPのステータスを決める。
 */
@Component
public class ErrorStatusResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorStatusResolver.class);

    private final Map<String, HttpStatus> statuses;

    public ErrorStatusResolver(List<ErrorStatusMapping> mappings) {
        Map<String, HttpStatus> merged = new HashMap<>();
        for (ErrorStatusMapping mapping : mappings) {
            mapping.statuses().forEach((code, status) -> {
                if (merged.putIfAbsent(code, status) != null) {
                    throw new IllegalStateException("エラーコードが重複しています：" + code);
                }
            });
        }
        this.statuses = Map.copyOf(merged);
    }

    public HttpStatus resolve(ErrorCode errorCode) {
        HttpStatus status = statuses.get(errorCode.code());
        if (status == null) {
            LOGGER.error("HTTPのステータスが登録されていないエラーコードです：{}", errorCode.code());
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return status;
    }
}
