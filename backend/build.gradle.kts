import org.flywaydb.core.Flyway
import org.jooq.codegen.GenerationTool
import org.jooq.meta.jaxb.Database
import org.jooq.meta.jaxb.Generate
import org.jooq.meta.jaxb.Generator
import org.jooq.meta.jaxb.Jdbc
import org.jooq.meta.jaxb.Target
import org.springframework.boot.gradle.plugin.SpringBootPlugin
import org.testcontainers.mysql.MySQLContainer
import org.testcontainers.utility.DockerImageName
import org.jooq.meta.jaxb.Configuration as JooqConfiguration

// コード生成の処理（generateJooq）で使う部品。
// バージョンは、アプリと同じSpring BootのBOMでそろえる（plugins の Spring Boot と同じ版にする）
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(platform("org.springframework.boot:spring-boot-dependencies:4.1.1"))
        classpath("org.jooq:jooq-codegen")
        classpath("org.flywaydb:flyway-mysql")
        classpath("com.mysql:mysql-connector-j")
        classpath("org.testcontainers:testcontainers-mysql")
    }
}

plugins {
    java
    id("org.springframework.boot") version "4.1.1"
}

group = "io.github.kawashima_study"
version = "0.0.1-SNAPSHOT"
description = "HoikuLink"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // ライブラリのバージョンは、Spring BootとSpring ModulithのBOMでまとめて決める
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))
    implementation(platform("org.springframework.modulith:spring-modulith-bom:2.1.1"))
    developmentOnly(platform(SpringBootPlugin.BOM_COORDINATES))

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.flywaydb:flyway-mysql")
    implementation("org.springframework.modulith:spring-modulith-observability-api")
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("org.springframework.modulith:spring-modulith-actuator")
    runtimeOnly("org.springframework.modulith:spring-modulith-observability-core")
    runtimeOnly("org.springframework.modulith:spring-modulith-runtime")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jooq-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-mysql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("user.timezone", "Asia/Tokyo")
}

// ---------------------------------------------------------------
// jOOQのコード生成
// 一時的なMySQLを起動し、マイグレーションでテーブルを作ってから、
// モジュールごとに、テーブルのクラスを生成する
// ---------------------------------------------------------------
val jooqModules = listOf("auth", "nursery", "diary")
val migrationDir = layout.projectDirectory.dir("src/main/resources/db/migration")
val jooqOutputDir = layout.buildDirectory.dir("generated-sources/jooq")

val generateJooq by tasks.registering {
    group = "jooq"
    description = "マイグレーションからテーブルを作り、jOOQのコードを生成する"
    inputs.dir(migrationDir)
    outputs.dir(jooqOutputDir)

    doLast {
        val outputDir = jooqOutputDir.get().asFile
        outputDir.deleteRecursively()

        MySQLContainer(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("hoikulink")
            .withUsername("root")
            .withPassword("codegen")
            .withCommand(
                "--character-set-server=utf8mb4",
                "--collation-server=utf8mb4_ja_0900_as_cs",
                "--default-time-zone=+09:00",
            )
            .use { mysql ->
                mysql.start()

                // アプリの起動時と同じく、共通（__root）→ 各モジュールの順に、
                // 実行の記録のテーブルを分けてマイグレーションする
                (listOf("__root") + jooqModules).forEach { module ->
                    val historyTable =
                        if (module == "__root") "flyway_schema_history" else "flyway_schema_history_$module"
                    Flyway.configure()
                        .dataSource(mysql.jdbcUrl, mysql.username, mysql.password)
                        .locations("filesystem:${migrationDir.asFile}/$module")
                        .table(historyTable)
                        .baselineOnMigrate(true)
                        .baselineVersion("0")
                        .load()
                        .migrate()
                }

                // モジュールごとに、自分のスキーマのテーブルだけを生成する
                jooqModules.forEach { module ->
                    GenerationTool.generate(
                        JooqConfiguration()
                            .withJdbc(
                                Jdbc()
                                    .withDriver("com.mysql.cj.jdbc.Driver")
                                    .withUrl(mysql.jdbcUrl)
                                    .withUser(mysql.username)
                                    .withPassword(mysql.password),
                            )
                            .withGenerator(
                                Generator()
                                    .withDatabase(
                                        Database()
                                            .withName("org.jooq.meta.mysql.MySQLDatabase")
                                            .withInputSchema(module),
                                    )
                                    .withGenerate(
                                        Generate()
                                            .withGeneratedAnnotation(false)
                                            .withJavaTimeTypes(true),
                                    )
                                    .withTarget(
                                        Target()
                                            .withPackageName(
                                                "io.github.kawashima_study.hoikulink.$module.adapter.out.jooq.generated",
                                            )
                                            .withDirectory(outputDir.absolutePath),
                                    ),
                            ),
                    )
                }
            }
    }
}

// 生成したコードを、アプリのコードと一緒にコンパイルする
sourceSets {
    main {
        java {
            srcDir(jooqOutputDir)
        }
    }
}

tasks.named("compileJava") {
    dependsOn(generateJooq)
}