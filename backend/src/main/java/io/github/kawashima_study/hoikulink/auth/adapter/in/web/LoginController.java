package io.github.kawashima_study.hoikulink.auth.adapter.in.web;

import io.github.kawashima_study.hoikulink.auth.application.LoginCommand;
import io.github.kawashima_study.hoikulink.auth.application.LoginResult;
import io.github.kawashima_study.hoikulink.auth.application.LoginUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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

    /** ログインのユースケース（実装のクラスではなくインターフェースで受け取る） */
    private final LoginUseCase loginUseCase;

    /** CSRF対策の確認 */
    private final CsrfGuard csrfGuard;

    /** 現在の時刻（Cookieの有効期間の計算に使う） */
    private final Clock clock;

    /**
     * ログインAPIの入口を作成
     *
     * @param loginUseCase ログインのユースケース
     * @param csrfGuard CSRF対策の確認
     * @param clock 現在の時刻
     */
    LoginController(LoginUseCase loginUseCase, CsrfGuard csrfGuard, Clock clock) {
        this.loginUseCase = loginUseCase;
        this.csrfGuard = csrfGuard;
        this.clock = clock;
    }

    /**
     * メールアドレスとパスワードでログインする
     *
     * @param request ログインのリクエストの本文
     * @param httpRequest HTTPのリクエスト（CSRF対策の確認に使う）
     * @return 成功の応答（本文にアクセストークン Cookieにリフレッシュトークン）
     * @throws CsrfCheckFailedException 独自の見出しがないまたは送信元が許可されていない場合（403）
     * @throws InvalidCredentialsException アカウントがないまたはパスワードが正しくない場合（401）
     * @throws AccountLockedException ロック中でパスワードが正しい場合（401）
     */
    @PostMapping("/login")
    ResponseEntity<AccessTokenResponse> login(
            @Valid @RequestBody LoginRequest request, // @Validで入力のチェックをする（誤りは422）
            HttpServletRequest httpRequest) {
        csrfGuard.check(httpRequest); // 他のサイトから攻撃者のアカウントでログインさせられるのを防ぐ
        LoginResult result = loginUseCase.login(new LoginCommand(request.email(), request.password()));
        return switch (result) { // sealedのため結果の種類を増やして扱い漏れがあるとコンパイルエラーになる
            case LoginResult.Success success ->
                AccessTokenResponse.ok(success.accessToken(), success.refreshToken(), cookieMaxAge(success));
            case LoginResult.InvalidCredentials invalid -> throw new InvalidCredentialsException();
            case LoginResult.AccountLocked locked -> throw new AccountLockedException();
        };
    }

    /**
     * リフレッシュトークンのCookieの有効期間を計算
     *
     * @param success ログインの成功の結果
     * @return 現在からリフレッシュトークンの有効期限までの時間
     */
    private Duration cookieMaxAge(LoginResult.Success success) {
        return Duration.between(Instant.now(clock), success.refreshTokenExpiresAt());
    }
}
