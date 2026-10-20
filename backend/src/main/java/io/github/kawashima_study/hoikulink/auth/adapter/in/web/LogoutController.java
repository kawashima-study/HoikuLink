package io.github.kawashima_study.hoikulink.auth.adapter.in.web;

import io.github.kawashima_study.hoikulink.auth.application.LogoutCommand;
import io.github.kawashima_study.hoikulink.auth.application.LogoutUseCase;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ログアウトAPIの入口
 */
@RestController
@RequestMapping("/api/v1/auth")
class LogoutController {

    /** ログアウトのユースケース */
    private final LogoutUseCase logoutUseCase;

    /** CSRF対策の確認 */
    private final CsrfGuard csrfGuard;

    /**
     * ログアウトAPIの入口を作成
     *
     * @param logoutUseCase ログアウトのユースケース
     * @param csrfGuard CSRF対策の確認
     */
    LogoutController(LogoutUseCase logoutUseCase, CsrfGuard csrfGuard) {
        this.logoutUseCase = logoutUseCase;
        this.csrfGuard = csrfGuard;
    }

    /**
     * 同じログインのリフレッシュトークンをすべて無効化しCookieを削除させる
     * <p>Cookieがない場合も204を返す（何度呼んでも同じ結果になる）
     *
     * @param refreshToken Cookieに入っていた生のリフレッシュトークン（ない場合はnull）
     * @param request HTTPのリクエスト（CSRF対策の確認に使う）
     * @return 本文のない204の応答（Cookieを削除させる見出し付き）
     * @throws CsrfCheckFailedException CSRF対策の確認に通らない場合（403）
     */
    @PostMapping("/logout")
    ResponseEntity<Void> logout(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken,
            HttpServletRequest request) {
        csrfGuard.check(request);
        if (refreshToken != null) {
            logoutUseCase.logout(new LogoutCommand(refreshToken));
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, RefreshTokenCookie.cleared().toString())
                .build();
    }
}
