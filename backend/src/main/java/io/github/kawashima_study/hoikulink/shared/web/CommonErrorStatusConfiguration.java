package io.github.kawashima_study.hoikulink.shared.web;

import io.github.kawashima_study.hoikulink.shared.CommonErrorCode;
import io.github.kawashima_study.hoikulink.shared.ErrorStatusMapping;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

/**
 * 共通のエラーコードとHTTPのステータスの対応表の設定
 */
@Configuration(proxyBeanMethods = false)
class CommonErrorStatusConfiguration {

    /** 入力内容の誤りを表すHTTPのステータス（422 Unprocessable Content） */
    private static final int UNPROCESSABLE_CONTENT = 422;

    /**
     * 共通のエラーコードとHTTPのステータスの対応表を作成
     *
     * @return 共通のエラーコードの対応表
     */
    @Bean
    ErrorStatusMapping commonErrorStatusMapping() {
        return ErrorStatusMapping.of(
                CommonErrorCode.class,
                Map.of(
                        CommonErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST,
                        CommonErrorCode.UNAUTHENTICATED, HttpStatus.UNAUTHORIZED,
                        CommonErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN,
                        CommonErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND,
                        CommonErrorCode.VALIDATION_ERROR, HttpStatus.valueOf(UNPROCESSABLE_CONTENT),
                        CommonErrorCode.TOO_MANY_REQUESTS, HttpStatus.TOO_MANY_REQUESTS,
                        CommonErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                        CommonErrorCode.SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE));
    }
}
