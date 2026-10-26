package io.github.kawashima_study.hoikulink.auth.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * Cookieを使うAPI（ログイン・再発行・ログアウト）のCSRF対策の確認
 * <p>独自の見出しと送信元（Origin）の両方を確認し他のサイトからのリクエストを断る
 */
@Component
class CsrfGuard {

    /** 画面が付ける独自の見出しの名前 */
    static final String CUSTOM_HEADER_NAME = "X-Requested-With";

    /** 画面が付ける独自の見出しの値 */
    static final String CUSTOM_HEADER_VALUE = "XMLHttpRequest";

    /** 許可する送信元（画面のドメイン） */
    private final String allowedOrigin;

    /**
     * CSRF対策の確認の部品を作成
     *
     * @param allowedOrigin 許可する送信元（例 http://localhost:5173）
     */
    CsrfGuard(@Value("${hoikulink.web.allowed-origin}") String allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
    }

    /**
     * 独自の見出しと送信元が正しいことを確認
     *
     * @param request HTTPのリクエスト
     * @throws CsrfCheckFailedException 独自の見出しがないまたは送信元が許可されていない場合（403）
     */
    void check(HttpServletRequest request) {
        // 他のサイトのフォームからは独自の見出しを付けられない
        boolean hasCustomHeader = CUSTOM_HEADER_VALUE.equals(request.getHeader(CUSTOM_HEADER_NAME));
        boolean fromAllowedOrigin = allowedOrigin.equals(request.getHeader(HttpHeaders.ORIGIN));
        if (!hasCustomHeader || !fromAllowedOrigin) {
            throw new CsrfCheckFailedException();
        }
    }
}
