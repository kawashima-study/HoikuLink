package io.github.kawashima_study.hoikulink.auth.adapter.in.web;

import io.github.kawashima_study.hoikulink.auth.application.RefreshCommand;
import io.github.kawashima_study.hoikulink.auth.application.RefreshResult;
import io.github.kawashima_study.hoikulink.auth.application.RefreshUseCase;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * アクセストークンの再発行APIの入口
 */
@RestController
@RequestMapping("/api/v1/auth")
class RefreshController {

    /** 再発行のユースケース */
    private final RefreshUseCase refreshUseCase;

    /** CSRF対策の確認 */
    private final CsrfGuard csrfGuard;

    /** 現在の時刻（Cookieの有効期間の計算に使う） */
    private final Clock clock;

    /**
     * 再発行APIの入口を作成
     *
     * @param refreshUseCase 再発行のユースケース
     * @param csrfGuard CSRF対策の確認
     * @param clock 現在の時刻
     */
    RefreshController(RefreshUseCase refreshUseCase, CsrfGuard csrfGuard, Clock clock) {
        this.refreshUseCase = refreshUseCase;
        this.csrfGuard = csrfGuard;
        this.clock = clock;
    }

    /**
     * Cookieのリフレッシュトークンでアクセストークンを再発行する
     *
     * @param refreshToken Cookieに入っていた生のリフレッシュトークン（ない場合はnull）
     * @param request HTTPのリクエスト（CSRF対策の確認に使う）
     * @return 成功の応答（本文にアクセストークン Cookieに交換したリフレッシュトークン）
     * @throws CsrfCheckFailedException CSRF対策の確認に通らない場合（403）
     * @throws RefreshTokenRejectedException 再発行できない場合（401）
     */
    @PostMapping("/refresh")
    ResponseEntity<AccessTokenResponse> refresh(
            // Cookieがなくても400にせずnullで受け取る
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken,
            HttpServletRequest request) {
        csrfGuard.check(request);
        if (refreshToken == null) {
            throw new RefreshTokenRejectedException();
        }
        RefreshResult result = refreshUseCase.refresh(new RefreshCommand(refreshToken));
        return switch (result) {
            case RefreshResult.Success success ->
                AccessTokenResponse.ok(success.accessToken(), success.refreshToken(), cookieMaxAge(success));
            case RefreshResult.Rejected rejected -> throw new RefreshTokenRejectedException();
        };
    }

    /**
     * リフレッシュトークンのCookieの有効期間を計算
     *
     * @param success 再発行の成功の結果
     * @return 現在からリフレッシュトークンの有効期限までの時間（交換しても延長しない）
     */
    private Duration cookieMaxAge(RefreshResult.Success success) {
        return Duration.between(Instant.now(clock), success.refreshTokenExpiresAt());
    }
}
