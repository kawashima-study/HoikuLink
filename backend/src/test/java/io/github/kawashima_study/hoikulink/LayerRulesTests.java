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

/**
 * モジュール内の層のルールのテスト（ArchUnit）
 * <p>モジュールどうしの依存は ModularityTests（Spring Modulith）で確認する
 */
@DisplayName("モジュール内の層のルール")
class LayerRulesTests {

    /** 確認の対象のパッケージの根元 */
    private static final String ROOT = "io.github.kawashima_study.hoikulink";

    /** 層のルールを確認するモジュール */
    private static final List<String> MODULES = List.of("auth", "nursery", "diary");

    /** 確認の対象のクラス（テストのクラスは含めない） */
    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    /**
     * モジュールごとに層の依存の向きが守られていることを確認
     * <p>どのモジュールで失敗したか分かるようモジュールごとに別のテストとして動かす
     *
     * @return モジュールごとのテスト
     */
    @TestFactory
    @DisplayName("層の依存の向きが守られている。")
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

    /**
     * domain のクラスがSpringとjOOQに依存しないことを確認
     */
    @Test
    @DisplayName("domainはSpring・jOOQに依存しない。")
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

    /**
     * 窓口の実装（名前が Adapter で終わるクラス）が public でないことを確認
     */
    @Test
    @DisplayName("窓口の実装はpublicにしない。")
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
