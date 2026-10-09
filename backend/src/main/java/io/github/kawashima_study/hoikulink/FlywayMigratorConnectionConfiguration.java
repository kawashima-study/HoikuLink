package io.github.kawashima_study.hoikulink;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayConnectionDetails;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Flywayだけ、マイグレーション用のユーザーで接続するための設定。
 *
 * Docker ComposeやTestcontainersの自動の接続（サービス接続）を使うと、
 * application.properties の spring.flyway.user・password より、
 * コンテナから作られた接続先の情報（アプリ用のユーザー）が優先される。
 * そこで、接続先のURLはアプリと同じものを使い、ユーザーとパスワードだけを
 * マイグレーション用に差し替えた接続先の情報を、優先（@Primary）で登録する。
 */
@Configuration(proxyBeanMethods = false)
class FlywayMigratorConnectionConfiguration {

    @Bean
    @Primary
    FlywayConnectionDetails migratorFlywayConnectionDetails(
            JdbcConnectionDetails jdbcConnectionDetails,
            @Value("${spring.flyway.user}") String user,
            @Value("${spring.flyway.password}") String password) {
        return new FlywayConnectionDetails() {

            @Override
            public String getJdbcUrl() {
                return jdbcConnectionDetails.getJdbcUrl();
            }

            @Override
            public String getUsername() {
                return user;
            }

            @Override
            public String getPassword() {
                return password;
            }
        };
    }
}
