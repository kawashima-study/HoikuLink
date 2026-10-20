package io.github.kawashima_study.hoikulink.auth.adapter.in.web;

import static io.github.kawashima_study.hoikulink.auth.adapter.out.jooq.generated.Tables.CREDENTIALS;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.kawashima_study.hoikulink.TestcontainersConfiguration;
import io.github.kawashima_study.hoikulink.shared.UserId;
import io.github.kawashima_study.hoikulink.shared.web.RequestIdFilter;
import jakarta.servlet.Filter;
import java.time.LocalDateTime;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * ログインAPI（LoginController）の結合テスト
 * <p>アプリ全体を起動しTestcontainersのMySQLに入れたアカウントでAPIを呼び出して確認する
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional // テストごとに入れたアカウントを取り消す（APIの処理も同じトランザクションの中で動く）
@DisplayName("ログインAPI")
class LoginControllerTests {

    /** ログインAPIのパス */
    private static final String LOGIN_PATH = "/api/v1/auth/login";

    /** メールアドレス（テストデータ） */
    private static final String EMAIL = "hanako@example.com";

    /** 正しいパスワード（テストデータ） */
    private static final String PASSWORD = "correct horse battery staple";

    /** テストのデータを入れた日時（日本時間） */
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 10, 20, 9, 0);

    /** 入力内容の誤りを表すHTTPのステータス（422） */
    private static final int UNPROCESSABLE_CONTENT = 422;

    /** 失敗回数の上限（ロックされる回数） */
    private static final byte LOCKED_FAILED_COUNT = 5;

    /** アプリ全体の部品（MockMvcを作るために使う） */
    @Autowired
    private WebApplicationContext context;

    /** テストのデータを直接入れるためのjOOQの入口 */
    @Autowired
    private DSLContext dsl;

    /** APIを呼び出すテスト用の部品（サーバーを起動せずにAPIを呼び出せる） */
    private MockMvc mockMvc;

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
    }

    /**
     * 正しいメールアドレスとパスワードでアクセストークンを返すことを確認
     *
     * @throws Exception APIの呼び出しに失敗した場合
     */
    @Test
    @DisplayName("正しいメールアドレスとパスワードならアクセストークンを返す。")
    void returnsAccessToken() throws Exception {
        // アカウントを入れる
        insertAccount(false);

        // ログインし本文の項目とトークンを保存させない見出しを確認
        login(EMAIL, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(300))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")));
    }

    /**
     * 正しいメールアドレスとパスワードでリフレッシュトークンのCookieを設定することを確認
     *
     * @throws Exception APIの呼び出しに失敗した場合
     */
    @Test
    @DisplayName("正しいメールアドレスとパスワードならリフレッシュトークンのCookieを設定する。")
    void setsRefreshTokenCookie() throws Exception {
        // アカウントを入れる
        insertAccount(false);

        // ログインしCookieの名前と属性を確認
        login(EMAIL, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/v1/auth")));
    }

    /**
     * パスワードが正しくない場合に401とINVALID_CREDENTIALSを返すことを確認
     *
     * @throws Exception APIの呼び出しに失敗した場合
     */
    @Test
    @DisplayName("パスワードが正しくなければ401とINVALID_CREDENTIALSを返す。")
    void returnsInvalidCredentialsForWrongPassword() throws Exception {
        // アカウントを入れる
        insertAccount(false);

        // 正しくないパスワードでログインしエラーコードとCookieがないことを確認
        login(EMAIL, "wrong password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    /**
     * アカウントがない場合にパスワードの誤りと同じ401とINVALID_CREDENTIALSを返すことを確認
     *
     * @throws Exception APIの呼び出しに失敗した場合
     */
    @Test
    @DisplayName("アカウントがなければパスワードの誤りと同じ401とINVALID_CREDENTIALSを返す。")
    void returnsInvalidCredentialsForUnknownEmail() throws Exception {
        // アカウントを入れずにログインしエラーコードを確認
        login("unknown@example.com", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    /**
     * ロック中でパスワードが正しい場合に401とACCOUNT_LOCKEDを返すことを確認
     *
     * @throws Exception APIの呼び出しに失敗した場合
     */
    @Test
    @DisplayName("ロック中でパスワードが正しければ401とACCOUNT_LOCKEDを返す。")
    void returnsAccountLockedForLockedAccount() throws Exception {
        // ロック中のアカウントを入れる
        insertAccount(true);

        // 正しいパスワードでログインしエラーコードを確認
        login(EMAIL, PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    /**
     * メールアドレスが空の場合に422とVALIDATION_ERRORを返すことを確認
     *
     * @throws Exception APIの呼び出しに失敗した場合
     */
    @Test
    @DisplayName("メールアドレスが空なら422とVALIDATION_ERRORを返す。")
    void returnsValidationErrorForBlankEmail() throws Exception {
        // 空白のみのメールアドレスでログインしエラーコードを確認
        login(" ", PASSWORD)
                .andExpect(status().is(UNPROCESSABLE_CONTENT))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    /**
     * パスワードが空の場合に422とVALIDATION_ERRORを返すことを確認
     *
     * @throws Exception APIの呼び出しに失敗した場合
     */
    @Test
    @DisplayName("パスワードが空なら422とVALIDATION_ERRORを返す。")
    void returnsValidationErrorForEmptyPassword() throws Exception {
        // 空文字列のパスワードでログインしエラーコードを確認
        login(EMAIL, "")
                .andExpect(status().is(UNPROCESSABLE_CONTENT))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    /**
     * メールアドレスの形式が正しくない場合に422とINVALID_EMAIL_ADDRESSを返すことを確認
     *
     * @throws Exception APIの呼び出しに失敗した場合
     */
    @Test
    @DisplayName("メールアドレスの形式が正しくなければ422とINVALID_EMAIL_ADDRESSを返す。")
    void returnsInvalidEmailAddress() throws Exception {
        // @のないメールアドレスでログインしエラーコードと入力した値が応答に含まれないことを確認
        login("hanako", PASSWORD)
                .andExpect(status().is(UNPROCESSABLE_CONTENT))
                .andExpect(jsonPath("$.code").value("INVALID_EMAIL_ADDRESS"))
                .andExpect(jsonPath("$.detail", not(containsString("hanako"))));
    }

    /**
     * ログインAPIを呼び出す
     *
     * @param email メールアドレス
     * @param password パスワード
     * @return APIの呼び出しの結果
     * @throws Exception APIの呼び出しに失敗した場合
     */
    private ResultActions login(String email, String password) throws Exception {
        String body = "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
        return mockMvc.perform(
                post(LOGIN_PATH).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /**
     * 正しいパスワードのアカウントをDBに入れる
     *
     * @param locked ロック中にする場合はtrue
     */
    private void insertAccount(boolean locked) {
        // 照合の形式（Argon2id）でハッシュ値を作る
        String passwordHash =
                Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8().encode(PASSWORD);
        String userId = UserId.newId().value();
        dsl.insertInto(CREDENTIALS)
                .set(CREDENTIALS.USER_ID, userId)
                .set(CREDENTIALS.EMAIL, EMAIL)
                .set(CREDENTIALS.PASSWORD_HASH, passwordHash)
                .set(CREDENTIALS.FAILED_LOGIN_COUNT, locked ? LOCKED_FAILED_COUNT : (byte) 0)
                .set(CREDENTIALS.LOCKED_AT, locked ? CREATED_AT : null)
                .set(CREDENTIALS.CREATED_AT, CREATED_AT)
                .set(CREDENTIALS.CREATED_BY, userId)
                .set(CREDENTIALS.UPDATED_AT, CREATED_AT)
                .set(CREDENTIALS.UPDATED_BY, userId)
                .execute();
    }
}
