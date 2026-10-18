package io.github.kawashima_study.hoikulink.auth.adapter.in.web;

import io.github.kawashima_study.hoikulink.auth.domain.AuthErrorCode;
import io.github.kawashima_study.hoikulink.shared.ErrorStatusMapping;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

/**
 * 認証モジュールのエラーコードとHTTPのステータスの対応表の設定
 */
@Configuration(proxyBeanMethods = false)
class AuthErrorStatusConfiguration {

    /** 入力内容の誤りを表すHTTPのステータス（422） */
    private static final int UNPROCESSABLE_CONTENT = 422;

    /**
     * 認証モジュールのエラーコードとHTTPのステータスの対応表を作成
     *
     * @return 認証モジュールのエラーコードの対応表
     */
    @Bean
    ErrorStatusMapping authErrorStatusMapping() {
        return ErrorStatusMapping.of(
                AuthErrorCode.class,
                Map.of(
                        AuthErrorCode.INVALID_EMAIL_ADDRESS, HttpStatus.valueOf(UNPROCESSABLE_CONTENT),
                        AuthErrorCode.SELF_UNLOCK_NOT_ALLOWED, HttpStatus.FORBIDDEN,
                        AuthErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED,
                        AuthErrorCode.ACCOUNT_LOCKED, HttpStatus.UNAUTHORIZED));
    }
}
