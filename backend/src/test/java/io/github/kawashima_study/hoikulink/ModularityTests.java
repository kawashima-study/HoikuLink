package io.github.kawashima_study.hoikulink;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * モジュールの構成のテスト（Spring Modulith）
 */
@DisplayName("モジュールの構成")
class ModularityTests {

    /** アプリケーションのモジュールの構成 */
    private final ApplicationModules modules = ApplicationModules.of(HoikuLinkApplication.class);

    /**
     * モジュールどうしの依存が許可された範囲に収まっていることを確認
     */
    @Test
    @DisplayName("モジュールの境界のルールが守られている。")
    void verifiesModularStructure() {
        modules.verify();
    }

    /**
     * モジュールの構成図を出力
     */
    @Test
    @DisplayName("モジュールの構成図を出力する。")
    void writesDocumentation() {
        new Documenter(modules).writeDocumentation();
    }
}
