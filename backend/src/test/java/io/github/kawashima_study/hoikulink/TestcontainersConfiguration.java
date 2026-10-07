package io.github.kawashima_study.hoikulink;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

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
                        "--default-time-zone=+00:00");
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> valkeyContainer() {
        return new GenericContainer<>(DockerImageName.parse("valkey/valkey:8"))
                .withExposedPorts(6379);
    }

}