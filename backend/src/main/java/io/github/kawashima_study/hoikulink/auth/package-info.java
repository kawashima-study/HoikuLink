/**
 * 認証モジュール
 * <p>ログインとログアウト トークン パスワード アカウントのロックを扱う
 * <p>共有のモジュール（shared）のみに依存する
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "認証",
        allowedDependencies = {"shared"})
package io.github.kawashima_study.hoikulink.auth;
