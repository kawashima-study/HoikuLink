package io.github.kawashima_study.hoikulink.auth.adapter.in.web;

import io.github.kawashima_study.hoikulink.auth.application.LoginCommand;
import io.github.kawashima_study.hoikulink.auth.application.LoginResult;
import io.github.kawashima_study.hoikulink.auth.application.LoginUseCase;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ログインAPIの入口
 * <p>ユースケースの結果をHTTPの応答に変換する
 */
@RestController
@RequestMapping("/api/v1/auth")
class LoginController {

    /** トークンの種類 */
    private static final String TOKEN_TYPE = "Bearer";

    /** ログインのユースケース（実装のクラスではなくインターフェースで受け取る） */
    private final LoginUseCase loginUseCase;

    /** 現在の時刻（Cookieの有効期間の計算に使う） */
    private final Clock clock;

    /**
     * ログインAPIの入口を作成
     *
     * @param loginUseCase ログインのユースケース
     * @param clock 現在の時刻
     */
    LoginController(LoginUseCase loginUseCase, Clock clock) {
        this.loginUseCase = loginUseCase;
        this.clock = clock;
    }

    /**
     * メールアドレスとパスワードでログインする
     *
     * @param request ログインのリクエストの本文
     * @return 成功の応答（本文にアクセストークン Cookieにリフレッシュトークン）
     * @throws InvalidCredentialsException アカウントがないまたはパスワードが正しくない場合（401）
     * @throws AccountLockedException ロック中でパスワードが正しい場合（401）
     */
    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) { // @Validで入力のチェックをする（誤りは422）
        LoginResult result = loginUseCase.login(new LoginCommand(request.email(), request.password()));
        return switch (result) { // sealedのため結果の種類を増やして扱い漏れがあるとコンパイルエラーになる
            case LoginResult.Success success -> ok(success);
            case LoginResult.InvalidCredentials invalid -> throw new InvalidCredentialsException();
            case LoginResult.AccountLocked locked -> throw new AccountLockedException();
        };
    }

    /**
     * ログインの成功の結果を応答に変換
     *
     * @param success ログインの成功の結果
     * @return 本文にアクセストークンを入れCookieにリフレッシュトークンを入れた応答
     */
    private ResponseEntity<LoginResponse> ok(LoginResult.Success success) {
        Duration maxAge = Duration.between(Instant.now(clock), success.refreshTokenExpiresAt());
        LoginResponse body = new LoginResponse(
                success.accessToken().value(),
                TOKEN_TYPE,
                success.accessToken().expiresIn().toSeconds());
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        RefreshTokenCookie.of(success.refreshToken(), maxAge).toString())
                .cacheControl(CacheControl.noStore()) // トークンを含む応答をブラウザや途中の機器に保存させない
                .body(body);
    }
}
