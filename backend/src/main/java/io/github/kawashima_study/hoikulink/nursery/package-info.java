/**
 * 園運営モジュール
 * <p>園 保育士 クラス 園児 保護者 役職と権限を扱う
 * <p>認証モジュールの窓口（api）のみに依存する
 */
@org.springframework.modulith.ApplicationModule(displayName = "園運営", allowedDependencies = "auth :: api")
package io.github.kawashima_study.hoikulink.nursery;
