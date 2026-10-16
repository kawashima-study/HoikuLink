package io.github.kawashima_study.hoikulink.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * リクエストごとにIDを作成しログ（MDC）と応答のヘッダーに入れるフィルター
 * <p>ログに任意の文字列が入るのを防ぐため利用者が送ったIDは使用しない
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    /** ログ（MDC）に入れるときのキー */
    public static final String MDC_KEY = "requestId";

    /** 応答のヘッダーの名前 */
    public static final String HEADER_NAME = "X-Request-Id";

    /**
     * リクエストIDを作成してログと応答のヘッダーに入れてから次の処理へ渡す
     * <p>スレッドが使い回されるため処理の後に必ずMDCから消す
     *
     * @param request リクエスト
     * @param response 応答
     * @param filterChain 次の処理
     * @throws ServletException 次の処理でサーブレットの例外が起きた場合
     * @throws IOException 次の処理で入出力の例外が起きた場合
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER_NAME, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
