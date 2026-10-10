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

@DisplayName("エラー応答（RFC 9457の形式）")
class GlobalExceptionHandlerTests {

    private static final String PROBLEM_JSON = "application/problem+json";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ErrorStatusResolver resolver =
                new ErrorStatusResolver(List.of(new CommonErrorStatusConfiguration().commonErrorStatusMapping()));
        mockMvc = MockMvcBuilders.standaloneSetup(new SampleController())
                .setControllerAdvice(new GlobalExceptionHandler(resolver))
                .addFilters(new RequestIdFilter())
                .build();
    }

    @Test
    @DisplayName("業務のエラーは、エラーコードに対応するステータスと、type・code・requestIdを返す")
    void returnsProblemForApplicationException() throws Exception {
        mockMvc.perform(get("/sample/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(header().exists(RequestIdFilter.HEADER_NAME))
                .andExpect(jsonPath("$.type").value("urn:hoikulink:error:resource-not-found"))
                .andExpect(jsonPath("$.title").value("対象が見つかりません"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("対象の連絡帳が見つかりません。"))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    @DisplayName("URLの中のIDの形式が違うときは、存在しないIDと同じく404を返す")
    void returnsNotFoundForInvalidId() throws Exception {
        mockMvc.perform(get("/sample/users/abc"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("入力チェックのエラーは、422と項目ごとの一覧を返す。入力された値は返さない")
    void returnsValidationErrors() throws Exception {
        mockMvc.perform(post("/sample/children")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].code").value("NOT_BLANK"))
                .andExpect(jsonPath("$.errors[0].rejectedValue").doesNotExist());
    }

    @Test
    @DisplayName("JSONが壊れているときは、400とINVALID_REQUESTを返す")
    void returnsBadRequestForBrokenJson() throws Exception {
        mockMvc.perform(post("/sample/children")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("想定外のエラーは、500を返し、内部の情報を返さない")
    void hidesInternalDetailsForUnexpectedError() throws Exception {
        mockMvc.perform(get("/sample/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").doesNotExist())
                .andExpect(content().string(not(containsString("secret-internal-message"))));
    }

    @RestController
    static class SampleController {

        @GetMapping("/sample/not-found")
        String notFound() {
            throw new ResourceNotFoundException("対象の連絡帳が見つかりません。");
        }

        @GetMapping("/sample/users/{userId}")
        String user(@PathVariable String userId) {
            return new UserId(userId).value();
        }

        @PostMapping("/sample/children")
        String create(@Valid @RequestBody ChildRequest request) {
            return request.name();
        }

        @GetMapping("/sample/unexpected")
        String unexpected() {
            throw new IllegalStateException("secret-internal-message");
        }
    }

    public record ChildRequest(@NotBlank String name) {}
}
