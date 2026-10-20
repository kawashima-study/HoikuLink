package io.github.kawashima_study.hoikulink.auth.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.kawashima_study.hoikulink.TestcontainersConfiguration;
import io.github.kawashima_study.hoikulink.shared.web.RequestIdFilter;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * アクセストークンの再発行API（RefreshController）の結合テスト
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
@DisplayName("アクセストークンの再発行API")
class RefreshControllerTests {

    /** 再発行APIのパス */
    private static final String REFRESH_PATH = "/api/v1/auth/refresh";

    /** アプリ全体の部品 */
    @Autowired
    private WebApplicationContext context;

    /** テストのデータを直接入れるためのjOOQの入口 */
    @Autowired
    private DSLContext dsl;

    /** APIを呼び出すテスト用の部品 */
    private MockMvc mockMvc;

    /** ログインと画面と同じ見出しでの呼び出しの部品 */
    private AuthApiTestSupport support;

    /**
     * 各テストの前にAPIを呼び出す部品を作成
     */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(
                        context.getBean(RequestIdFilter.class),
                        context.getBean("springSecurityFilterChain", Filter.class))
                .build();
        support = new AuthApiTestSupport(dsl, mockMvc);
    }

    /**
     * 使えるリフレッシュトークンで新しいアクセストークンと交換したリフレッシュトークンを返すことを確認
     *
     * @throws Exception APIの呼び出しに失敗した場合
     */
    @Test
    @DisplayName("使えるリフレッシュトークンなら新しいアクセストークンと交換したリフレッシュトークンを返す。")
    void returnsNewTokens() throws Exception {
        // ログインしてリフレッシュトークンを受け取る
        String refreshToken = support.loginAndGetRefreshToken();

        // 再発行
        String setCookie = support.postFromScreen(REFRESH_PATH, refreshToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.SET_COOKIE);

        // Cookieのリフレッシュトークンが新しい値に変わっていることを確認
        assertThat(AuthApiTestSupport.refreshTokenOf(setCookie)).isNotEqualTo(refreshToken);
    }

    /**
     * 交換前のリフレッシュトークンをもう一度使うと401になり交換後のトークンも使えなくなることを確認
     *
     * @throws Exception APIの呼び出しに失敗した場合
     */
    @Test
    @DisplayName("交換前のトークンをもう一度使うと401になり交換後のトークンも使えなくなる。")
    void revokesFamilyWhenReused() throws Exception {
        // ログインして1回再発行し交換後のトークンを受け取る
        String oldToken = support.loginAndGetRefreshToken();
        String setCookie = support.postFromScreen(REFRESH_PATH, oldToken)
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.SET_COOKIE);
        String newToken = AuthApiTestSupport.refreshTokenOf(setCookie);

        // 交換前のトークンをもう一度使うと401になることを確認
        support.postFromScreen(REFRESH_PATH, oldToken)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        // 交換後のトークンも使えなくなっていることを確認
        support.postFromScreen(REFRESH_PATH, newToken).andExpect(status().isUnauthorized());
    }

    /**
     * Cookieがない場合に401になることを確認
     *
     * @throws Exception APIの呼び出しに失敗した場合
     */
    @Test
    @DisplayName("Cookieがなければ401を返す。")
    void rejectsWithoutCookie() throws Exception {
        // Cookieなしで再発行すると401になることを確認
        support.postFromScreen(REFRESH_PATH, null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    /**
     * 独自の見出しがない場合に403になることを確認
     *
     * @throws Exception APIの呼び出しに失敗した場合
     */
    @Test
    @DisplayName("独自の見出しがなければ403を返す。")
    void rejectsWithoutCustomHeader() throws Exception {
        // ログインしてリフレッシュトークンを受け取る
        String refreshToken = support.loginAndGetRefreshToken();

        // 独自の見出しを付けずに再発行すると403になることを確認
        mockMvc.perform(post(REFRESH_PATH)
                        .header(HttpHeaders.ORIGIN, AuthApiTestSupport.ALLOWED_ORIGIN)
                        .cookie(new Cookie(RefreshTokenCookie.NAME, refreshToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    /**
     * 許可していない送信元からの場合に403になることを確認
     *
     * @throws Exception APIの呼び出しに失敗した場合
     */
    @Test
    @DisplayName("許可していない送信元からなら403を返す。")
    void rejectsOtherOrigin() throws Exception {
        // ログインしてリフレッシュトークンを受け取る
        String refreshToken = support.loginAndGetRefreshToken();

        // 他のサイトを送信元として再発行すると403になることを確認
        mockMvc.perform(post(REFRESH_PATH)
                        .header(CsrfGuard.CUSTOM_HEADER_NAME, CsrfGuard.CUSTOM_HEADER_VALUE)
                        .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                        .cookie(new Cookie(RefreshTokenCookie.NAME, refreshToken)))
                .andExpect(status().isForbidden());
    }
}
