package io.github.kawashima_study.hoikulink.shared.web;

import io.github.kawashima_study.hoikulink.shared.CommonErrorCode;
import io.github.kawashima_study.hoikulink.shared.ErrorStatusMapping;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

/**
 * 共通のエラーコードとHTTPのステータスの対応表。
 */
@Configuration(proxyBeanMethods = false)
class CommonErrorStatusConfiguration {

    private static final int UNPROCESSABLE_CONTENT = 422;

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
