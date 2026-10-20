package io.github.kawashima_study.hoikulink.auth.adapter.in.web;

import static io.github.kawashima_study.hoikulink.auth.adapter.out.jooq.generated.Tables.CREDENTIALS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import io.github.kawashima_study.hoikulink.shared.UserId;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import org.jooq.DSLContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * 認証のAPIの結合テストで共通に使う準備と呼び出し
 * <p>アカウントを入れてログインしリフレッシュトークンのCookieを取り出す
 */
final class AuthApiTestSupport {

    /** 画面のドメイン（テストの設定で許可している送信元） */
    static final String ALLOWED_ORIGIN = "http://localhost:5173";

    /** メールアドレス（テストデータ） */
    static final String EMAIL = "hanako@example.com";

    /** 正しいパスワード（テストデータ） */
    static final String PASSWORD = "correct horse battery staple";

    /** テストのデータを入れた日時（日本時間） */
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 10, 20, 9, 0);

    /** テストのデータを直接入れるためのjOOQの入口 */
    private final DSLContext dsl;

    /** APIを呼び出すテスト用の部品 */
    private final MockMvc mockMvc;

    /**
     * 準備と呼び出しの部品を作成
     *
     * @param dsl テストのデータを直接入れるためのjOOQの入口
     * @param mockMvc APIを呼び出すテスト用の部品
     */
    AuthApiTestSupport(DSLContext dsl, MockMvc mockMvc) {
        this.dsl = dsl;
        this.mockMvc = mockMvc;
    }

    /**
     * アカウントを入れてログインしリフレッシュトークンを取り出す
     *
     * @return ログインで発行された生のリフレッシュトークン
     * @throws Exception APIの呼び出しに失敗した場合
     */
    String loginAndGetRefreshToken() throws Exception {
        insertAccount();
        String body = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(EMAIL, PASSWORD);
        String setCookie = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.SET_COOKIE);
        return refreshTokenOf(setCookie);
    }

    /**
     * 画面と同じ見出し（独自の見出しと送信元）を付けてPOSTする
     *
     * @param path APIのパス
     * @param refreshToken Cookieに入れる生のリフレッシュトークン（nullの場合はCookieを付けない）
     * @return APIの呼び出しの結果
     * @throws Exception APIの呼び出しに失敗した場合
     */
    ResultActions postFromScreen(String path, String refreshToken) throws Exception {
        MockHttpServletRequestBuilder request = post(path)
                .header(CsrfGuard.CUSTOM_HEADER_NAME, CsrfGuard.CUSTOM_HEADER_VALUE)
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN);
        if (refreshToken != null) {
            request.cookie(new Cookie(RefreshTokenCookie.NAME, refreshToken));
        }
        return mockMvc.perform(request);
    }

    /**
     * Set-Cookieの見出しからリフレッシュトークンの値を取り出す
     *
     * @param setCookie Set-Cookieの見出しの値（例 refresh_token=abc; Path=/api/v1/auth; ...）
     * @return リフレッシュトークンの値
     */
    static String refreshTokenOf(String setCookie) {
        String prefix = RefreshTokenCookie.NAME + "=";
        return setCookie.substring(prefix.length(), setCookie.indexOf(';')); // 名前の後ろから最初の;の前までが値
    }

    /**
     * 正しいパスワードのアカウントをDBに入れる
     */
    private void insertAccount() {
        String passwordHash =
                Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8().encode(PASSWORD);
        String userId = UserId.newId().value();
        dsl.insertInto(CREDENTIALS)
                .set(CREDENTIALS.USER_ID, userId)
                .set(CREDENTIALS.EMAIL, EMAIL)
                .set(CREDENTIALS.PASSWORD_HASH, passwordHash)
                .set(CREDENTIALS.CREATED_AT, CREATED_AT)
                .set(CREDENTIALS.CREATED_BY, userId)
                .set(CREDENTIALS.UPDATED_AT, CREATED_AT)
                .set(CREDENTIALS.UPDATED_BY, userId)
                .execute();
    }
}
