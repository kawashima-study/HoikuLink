package io.github.kawashima_study.hoikulink.auth.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.kawashima_study.hoikulink.TestcontainersConfiguration;
import io.github.kawashima_study.hoikulink.shared.web.RequestIdFilter;
import jakarta.servlet.Filter;
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
 * ログアウトAPI（LogoutController）の結合テスト
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
@DisplayName("ログアウトAPI")
class LogoutControllerTests {

    /** ログアウトAPIのパス */
    private static final String LOGOUT_PATH = "/api/v1/auth/logout";

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
     * ログアウトすると204を返しCookieを削除させることを確認
     *
     * @throws Exception APIの呼び出しに失敗した場合
     */
    @Test
    @DisplayName("ログアウトすると204を返しCookieを削除させる。")
    void returnsNoContentAndClearsCookie() throws Exception {
        // ログインしてリフレッシュトークンを受け取る
        String refreshToken = support.loginAndGetRefreshToken();

        // ログアウトし有効期間0のCookieが返ることを確認
        support.postFromScreen(LOGOUT_PATH, refreshToken)
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }

    /**
     * ログアウトした後はリフレッシュトークンで再発行できないことを確認
     *
     * @throws Exception APIの呼び出しに失敗した場合
     */
    @Test
    @DisplayName("ログアウトした後はリフレッシュトークンで再発行できない。")
    void refreshFailsAfterLogout() throws Exception {
        // ログインしてからログアウト
        String refreshToken = support.loginAndGetRefreshToken();
        support.postFromScreen(LOGOUT_PATH, refreshToken);

        // 同じリフレッシュトークンで再発行すると401になることを確認
        support.postFromScreen("/api/v1/auth/refresh", refreshToken).andExpect(status().isUnauthorized());
    }

    /**
     * Cookieがない場合も204を返すことを確認
     *
     * @throws Exception APIの呼び出しに失敗した場合
     */
    @Test
    @DisplayName("Cookieがなくても204を返す。")
    void returnsNoContentWithoutCookie() throws Exception {
        // Cookieなしでログアウトしても204になることを確認
        support.postFromScreen(LOGOUT_PATH, null).andExpect(status().isNoContent());
    }

    /**
     * 独自の見出しがない場合に403になることを確認
     *
     * @throws Exception APIの呼び出しに失敗した場合
     */
    @Test
    @DisplayName("独自の見出しがなければ403を返す。")
    void rejectsWithoutCustomHeader() throws Exception {
        // 独自の見出しを付けずにログアウトすると403になることを確認
        mockMvc.perform(post(LOGOUT_PATH).header(HttpHeaders.ORIGIN, AuthApiTestSupport.ALLOWED_ORIGIN))
                .andExpect(status().isForbidden());
    }

    /**
     * 許可していない送信元からの場合に403になることを確認
     *
     * @throws Exception APIの呼び出しに失敗した場合
     */
    @Test
    @DisplayName("許可していない送信元からなら403を返す。")
    void rejectsOtherOrigin() throws Exception {
        // 他のサイトを送信元としてログアウトすると403になることを確認
        mockMvc.perform(post(LOGOUT_PATH)
                        .header(CsrfGuard.CUSTOM_HEADER_NAME, CsrfGuard.CUSTOM_HEADER_VALUE)
                        .header(HttpHeaders.ORIGIN, "https://evil.example.com"))
                .andExpect(status().isForbidden());
    }

    /**
     * 送信元の見出しがない場合に403になることを確認
     *
     * @throws Exception APIの呼び出しに失敗した場合
     */
    @Test
    @DisplayName("送信元の見出しがなければ403を返す。")
    void rejectsWithoutOrigin() throws Exception {
        // 送信元の見出しを付けずにログアウトすると403になることを確認
        mockMvc.perform(post(LOGOUT_PATH).header(CsrfGuard.CUSTOM_HEADER_NAME, CsrfGuard.CUSTOM_HEADER_VALUE))
                .andExpect(status().isForbidden());
    }
}
