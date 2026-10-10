package io.github.kawashima_study.hoikulink.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.kawashima_study.hoikulink.shared.CommonErrorCode;
import io.github.kawashima_study.hoikulink.shared.ErrorStatusMapping;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@DisplayName("エラーコードからHTTPのステータスを決める部品")
class ErrorStatusResolverTests {

    private final ErrorStatusMapping commonMapping = new CommonErrorStatusConfiguration().commonErrorStatusMapping();

    @Test
    @DisplayName("共通のエラーコードを、API一覧のとおりのステータスにする")
    void resolvesCommonCodes() {
        ErrorStatusResolver resolver = new ErrorStatusResolver(List.of(commonMapping));

        assertThat(resolver.resolve(CommonErrorCode.INVALID_REQUEST)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resolver.resolve(CommonErrorCode.RESOURCE_NOT_FOUND)).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resolver.resolve(CommonErrorCode.VALIDATION_ERROR).value()).isEqualTo(422);
        assertThat(resolver.resolve(CommonErrorCode.INTERNAL_ERROR)).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("同じエラーコードが2つのモジュールで登録されていたら、起動できない")
    void rejectsDuplicatedCodes() {
        assertThatThrownBy(() -> new ErrorStatusResolver(List.of(commonMapping, commonMapping)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("重複");
    }

    @Test
    @DisplayName("対応表に登録されていないエラーコードは、500にする")
    void resolvesUnknownCodeAsInternalError() {
        ErrorStatusResolver resolver = new ErrorStatusResolver(List.of());

        assertThat(resolver.resolve(CommonErrorCode.FORBIDDEN)).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
