package io.github.kawashima_study.hoikulink.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.kawashima_study.hoikulink.shared.CommonErrorCode;
import io.github.kawashima_study.hoikulink.shared.ErrorStatusMapping;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * エラーコードからHTTPのステータスを決める部品（ErrorStatusResolver）のテスト
 */
@DisplayName("エラーコードからHTTPのステータスを決める部品")
class ErrorStatusResolverTests {

    /** 共通のエラーコードの対応表 */
    private final ErrorStatusMapping commonMapping = new CommonErrorStatusConfiguration().commonErrorStatusMapping();

    /**
     * 共通のエラーコードがAPI一覧のとおりのステータスになることを確認
     */
    @Test
    @DisplayName("共通のエラーコードをAPI一覧のとおりのステータスにする。")
    void resolvesCommonCodes() {
        // 共通の対応表のみで部品を作成
        ErrorStatusResolver resolver = new ErrorStatusResolver(List.of(commonMapping));

        // 代表的なエラーコードのステータスを確認
        assertThat(resolver.resolve(CommonErrorCode.INVALID_REQUEST)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resolver.resolve(CommonErrorCode.RESOURCE_NOT_FOUND)).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resolver.resolve(CommonErrorCode.VALIDATION_ERROR).value()).isEqualTo(422);
        assertThat(resolver.resolve(CommonErrorCode.INTERNAL_ERROR)).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 同じエラーコードが2つの対応表に登録されていると部品を作成できないことを確認
     */
    @Test
    @DisplayName("同じエラーコードが2つのモジュールで登録されていたら起動できない。")
    void rejectsDuplicatedCodes() {
        // 同じ対応表を2つ渡すと例外になりメッセージに重複と含まれることを確認
        assertThatThrownBy(() -> new ErrorStatusResolver(List.of(commonMapping, commonMapping)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("重複");
    }

    /**
     * 対応表に登録されていないエラーコードが500になることを確認
     */
    @Test
    @DisplayName("対応表に登録されていないエラーコードは500にする。")
    void resolvesUnknownCodeAsInternalError() {
        // 対応表なしで部品を作成
        ErrorStatusResolver resolver = new ErrorStatusResolver(List.of());

        // 登録されていないエラーコードが500になることを確認
        assertThat(resolver.resolve(CommonErrorCode.FORBIDDEN)).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
