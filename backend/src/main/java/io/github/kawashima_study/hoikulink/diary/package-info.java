/**
 * 連絡帳モジュール
 * <p>保育士連絡帳と保護者連絡帳を扱う
 * <p>園運営モジュールの窓口（api）と型（types）のみに依存する
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "連絡帳",
        allowedDependencies = {"nursery :: api", "nursery :: types"})
package io.github.kawashima_study.hoikulink.diary;
