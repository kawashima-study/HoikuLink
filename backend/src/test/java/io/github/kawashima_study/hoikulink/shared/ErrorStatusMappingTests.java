package io.github.kawashima_study.hoikulink.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * エラーコードとHTTPのステータスの対応表（ErrorStatusMapping）のテスト
 */
@DisplayName("エラーコードとHTTPのステータスの対応表")
class ErrorStatusMappingTests {

    /**
     * テストで使用するエラーコード
     */
    enum SampleErrorCode implements ErrorCode {

        /** 1つ目のエラーコード */
        FIRST,

        /** 2つ目のエラーコード */
        SECOND;

        /**
         * エラーコードの名前を返す
         *
         * @return エラーコードの名前
         */
        @Override
        public String code() {
            return name();
        }

        /**
         * エラーコードの名前を説明として返す
         *
         * @return エラーコードの名前
         */
        @Override
        public String title() {
            return name();
        }
    }

    /**
     * すべてのエラーコードにステータスがあれば対応表を作成できることを確認
     */
    @Test
    @DisplayName("すべてのエラーコードにステータスがあれば対応表を作れる。")
    void createsMappingWhenAllCodesHaveStatus() {
        // 2つのエラーコードの両方にステータスを決めて対応表を作成
        ErrorStatusMapping mapping = ErrorStatusMapping.of(
                SampleErrorCode.class,
                Map.of(SampleErrorCode.FIRST, HttpStatus.CONFLICT, SampleErrorCode.SECOND, HttpStatus.NOT_FOUND));

        // 対応表にどちらのエラーコードも入っていることを確認
        assertThat(mapping.statuses())
                .containsEntry("FIRST", HttpStatus.CONFLICT)
                .containsEntry("SECOND", HttpStatus.NOT_FOUND);
    }

    /**
     * ステータスが決まっていないエラーコードがあれば対応表を作成できないことを確認
     * <p>書き忘れをアプリケーションの起動時に見つけるため
     */
    @Test
    @DisplayName("ステータスが決まっていないエラーコードがあれば対応表を作れない。")
    void rejectsMappingWithMissingCode() {
        // SECOND のステータスを決めずに作成すると例外になりメッセージに SECOND が含まれることを確認
        assertThatThrownBy(() -> ErrorStatusMapping.of(
                        SampleErrorCode.class, Map.of(SampleErrorCode.FIRST, HttpStatus.CONFLICT)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SECOND");
    }
}
