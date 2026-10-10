package io.github.kawashima_study.hoikulink.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@DisplayName("エラーコードとHTTPのステータスの対応表")
class ErrorStatusMappingTests {

    enum SampleErrorCode implements ErrorCode {
        FIRST,
        SECOND;

        @Override
        public String code() {
            return name();
        }

        @Override
        public String title() {
            return name();
        }
    }

    @Test
    @DisplayName("すべてのエラーコードにステータスがあれば、対応表を作れる")
    void createsMappingWhenAllCodesHaveStatus() {
        ErrorStatusMapping mapping = ErrorStatusMapping.of(
                SampleErrorCode.class,
                Map.of(SampleErrorCode.FIRST, HttpStatus.CONFLICT, SampleErrorCode.SECOND, HttpStatus.NOT_FOUND));

        assertThat(mapping.statuses())
                .containsEntry("FIRST", HttpStatus.CONFLICT)
                .containsEntry("SECOND", HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("ステータスが決まっていないエラーコードがあれば、作れない（書き忘れを起動時に見つける）")
    void rejectsMappingWithMissingCode() {
        assertThatThrownBy(() -> ErrorStatusMapping.of(
                        SampleErrorCode.class, Map.of(SampleErrorCode.FIRST, HttpStatus.CONFLICT)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SECOND");
    }
}
