package io.github.kawashima_study.hoikulink;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Securityの設定
 * <p>Spring Securityが入っていると何も設定しない場合はすべてのAPIにBasic認証が掛かるため明示的に設定する
 * <p>今はAccess Tokenで守るAPIがないためすべて許可する（Access Tokenの確認は園の選択のAPIと一緒に追加する）
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

    /**
     * リクエストの確認の流れを部品として登録する
     *
     * @param http Spring Securityの設定の入口
     * @return リクエストの確認の流れ
     * @throws Exception 設定に失敗した場合
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable) // CSRF対策はCookieを使うAPIでCsrfGuardが行う
                .httpBasic(AbstractHttpConfigurer::disable) // ブラウザのBasic認証のポップアップを出さない
                .formLogin(AbstractHttpConfigurer::disable) // Spring Securityの標準のログイン画面を使わない
                .logout(AbstractHttpConfigurer::disable) // ログアウトはLogoutControllerで行う
                .build();
    }
}
