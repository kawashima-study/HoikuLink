package io.github.kawashima_study.hoikulink.shared.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.kawashima_study.hoikulink.shared.ResourceNotFoundException;
import io.github.kawashima_study.hoikulink.shared.UserId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * エラー応答（GlobalExceptionHandler）のテスト
 */
@DisplayName("エラー応答（RFC 9457の形式）")
class GlobalExceptionHandlerTests {

    /** RFC 9457の形式の応答の種類 */
    private static final String PROBLEM_JSON = "application/problem+json";

    /** APIを呼び出すためのテスト用の道具 */
    private MockMvc mockMvc;

    /**
     * テスト用のコントローラーと例外の変換の部品とリクエストIDのフィルターを組み立てる
     */
    @BeforeEach
    void setUp() {
        ErrorStatusResolver resolver =
                new ErrorStatusResolver(List.of(new CommonErrorStatusConfiguration().commonErrorStatusMapping()));
        mockMvc = MockMvcBuilders.standaloneSetup(new SampleController())
                .setControllerAdvice(new GlobalExceptionHandler(resolver))
                .addFilters(new RequestIdFilter())
                .build();
    }

    /**
     * 業務のエラーがエラーコードに対応するステータスと type code requestId を返すことを確認
     *
     * @throws Exception APIの呼び出しで例外が起きた場合
     */
    @Test
    @DisplayName("業務のエラーはエラーコードに対応するステータスとtype・code・requestIdを返す。")
    void returnsProblemForApplicationException() throws Exception {
        // 対象が見つからないエラーを返すAPIを呼び出し応答の各項目を確認
        mockMvc.perform(get("/sample/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(header().exists(RequestIdFilter.HEADER_NAME))
                .andExpect(jsonPath("$.type").value("urn:hoikulink:error:resource-not-found"))
                .andExpect(jsonPath("$.title").value("対象が見つかりません。"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("対象の連絡帳が見つかりません。"))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    /**
     * URLの中のIDの形式が違うときに存在しないIDと同じく404を返すことを確認
     *
     * @throws Exception APIの呼び出しで例外が起きた場合
     */
    @Test
    @DisplayName("URLの中のIDの形式が違うときは存在しないIDと同じく404を返す。")
    void returnsNotFoundForInvalidId() throws Exception {
        // 形式の違うIDでAPIを呼び出し404と RESOURCE_NOT_FOUND になることを確認
        mockMvc.perform(get("/sample/users/abc"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    /**
     * 入力チェックのエラーが422と項目ごとの一覧を返し入力された値を返さないことを確認
     *
     * @throws Exception APIの呼び出しで例外が起きた場合
     */
    @Test
    @DisplayName("入力チェックのエラーは422と項目ごとの一覧を返し入力された値は返さない。")
    void returnsValidationErrors() throws Exception {
        // 名前が空白のみのリクエストを送り応答を確認
        mockMvc.perform(post("/sample/children")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].code").value("NOT_BLANK"))
                .andExpect(jsonPath("$.errors[0].rejectedValue").doesNotExist());
    }

    /**
     * JSONが壊れているときに400と INVALID_REQUEST を返すことを確認
     *
     * @throws Exception APIの呼び出しで例外が起きた場合
     */
    @Test
    @DisplayName("JSONが壊れているときは400とINVALID_REQUESTを返す。")
    void returnsBadRequestForBrokenJson() throws Exception {
        // 閉じていないJSONを送り応答を確認
        mockMvc.perform(post("/sample/children")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    /**
     * 想定外のエラーが500を返し内部の情報を返さないことを確認
     *
     * @throws Exception APIの呼び出しで例外が起きた場合
     */
    @Test
    @DisplayName("想定外のエラーは500を返し内部の情報を返さない。")
    void hidesInternalDetailsForUnexpectedError() throws Exception {
        // 想定外のエラーを起こすAPIを呼び出し内部のメッセージが応答に含まれないことを確認
        mockMvc.perform(get("/sample/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").doesNotExist())
                .andExpect(content().string(not(containsString("secret-internal-message"))));
    }

    /**
     * テスト用のコントローラー
     */
    @RestController
    static class SampleController {

        /**
         * 対象が見つからないエラーを起こす
         *
         * @return 返らない
         */
        @GetMapping("/sample/not-found")
        String notFound() {
            throw new ResourceNotFoundException("対象の連絡帳が見つかりません。");
        }

        /**
         * URLの中のIDからユーザーIDを作成する
         *
         * @param userId URLの中のID
         * @return ユーザーIDの文字列
         */
        @GetMapping("/sample/users/{userId}")
        String user(@PathVariable String userId) {
            return new UserId(userId).value();
        }

        /**
         * 入力チェックをしてリクエストを受け付ける
         *
         * @param request 入力チェックの対象のリクエスト
         * @return 受け付けた名前
         */
        @PostMapping("/sample/children")
        String create(@Valid @RequestBody ChildRequest request) {
            return request.name();
        }

        /**
         * 想定外のエラーを起こす
         *
         * @return 返らない
         */
        @GetMapping("/sample/unexpected")
        String unexpected() {
            throw new IllegalStateException("secret-internal-message");
        }
    }

    /**
     * テスト用のリクエスト
     *
     * @param name 空白のみを受け付けない名前
     */
    public record ChildRequest(@NotBlank String name) {}
}
