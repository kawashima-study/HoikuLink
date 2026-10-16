package io.github.kawashima_study.hoikulink;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * テストで使用するコンテナ（MySQLとValkey）の設定
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    /**
     * 手元と同じ設定のMySQLのコンテナを作成
     * <p>DBのユーザーの作成の SQL をコンテナの起動時に実行する
     *
     * @return MySQLのコンテナ
     */
    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return new MySQLContainer(DockerImageName.parse("mysql:8.4"))
                .withDatabaseName("hoikulink")
                .withUsername("hoikulink_app")
                .withPassword("localapppassword")
                .withCopyFileToContainer(
                        MountableFile.forHostPath("docker/mysql/init/01-create-users.sql"),
                        "/docker-entrypoint-initdb.d/01-create-users.sql")
                .withCommand(
                        "--character-set-server=utf8mb4",
                        "--collation-server=utf8mb4_ja_0900_as_cs",
                        "--default-time-zone=+09:00");
    }

    /**
     * Valkey（Redis互換）のコンテナを作成
     *
     * @return Valkeyのコンテナ
     */
    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> valkeyContainer() {
        return new GenericContainer<>(DockerImageName.parse("valkey/valkey:8")).withExposedPorts(6379);
    }
}
