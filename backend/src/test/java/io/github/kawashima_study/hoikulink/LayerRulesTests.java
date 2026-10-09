package io.github.kawashima_study.hoikulink;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

@DisplayName("モジュール内の層のルール")
class LayerRulesTests {

    private static final String ROOT = "io.github.kawashima_study.hoikulink";
    private static final List<String> MODULES = List.of("auth", "nursery", "diary");

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @TestFactory
    @DisplayName("層の依存の向きが守られている")
    Stream<DynamicTest> layersAreRespected() {
        return MODULES.stream()
                .map(module -> DynamicTest.dynamicTest(module, () -> {
                    String base = ROOT + "." + module;
                    layeredArchitecture()
                            .consideringOnlyDependenciesInLayers()
                            .withOptionalLayers(true)
                            .layer("Api")
                            .definedBy(base + ".api..")
                            .layer("Types")
                            .definedBy(base + ".types..")
                            .layer("Application")
                            .definedBy(base + ".application..")
                            .layer("Domain")
                            .definedBy(base + ".domain..")
                            .layer("Adapter")
                            .definedBy(base + ".adapter..")
                            .whereLayer("Api")
                            .mayNotBeAccessedByAnyLayer()
                            .whereLayer("Adapter")
                            .mayNotBeAccessedByAnyLayer()
                            .whereLayer("Application")
                            .mayOnlyBeAccessedByLayers("Api", "Adapter")
                            .whereLayer("Domain")
                            .mayOnlyBeAccessedByLayers("Application", "Adapter")
                            .whereLayer("Types")
                            .mayOnlyBeAccessedByLayers("Api", "Application", "Domain", "Adapter")
                            .check(classes);
                }));
    }

    @Test
    @DisplayName("domainはSpring・jOOQに依存しない")
    void domainDoesNotDependOnFrameworks() {
        noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "org.jooq..")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("窓口の実装はpublicにしない")
    void apiAdaptersAreNotPublic() {
        classes()
                .that()
                .resideInAPackage("..api..")
                .and()
                .haveSimpleNameEndingWith("Adapter")
                .should()
                .notBePublic()
                .allowEmptyShould(true)
                .check(classes);
    }
}
