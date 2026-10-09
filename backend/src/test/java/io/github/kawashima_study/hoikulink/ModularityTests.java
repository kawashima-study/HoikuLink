package io.github.kawashima_study.hoikulink;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

@DisplayName("モジュールの構成")
class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(HoikuLinkApplication.class);

    @Test
    @DisplayName("モジュールの境界のルールが守られている")
    void verifiesModularStructure() {
        modules.verify();
    }

    @Test
    @DisplayName("モジュールの構成図を出力する")
    void writesDocumentation() {
        new Documenter(modules).writeDocumentation();
    }
}
