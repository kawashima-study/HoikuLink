package io.github.kawashima_study.hoikulink;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayConnectionDetails;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Flywayのみマイグレーション用のユーザーで接続するための設定
 * <p>Docker ComposeやTestcontainersの自動の接続（サービス接続）を使うと
 * application.properties の spring.flyway.user と password より
 * コンテナから作られた接続先の情報（アプリ用のユーザー）が優先される
 * <p>そのため接続先のURLはアプリと同じものを使いユーザーとパスワードのみを
 * マイグレーション用に差し替えた接続先の情報を優先（@Primary）で登録する
 */
@Configuration(proxyBeanMethods = false)
class FlywayMigratorConnectionConfiguration {

    /**
     * マイグレーション用のユーザーで接続する接続先の情報を作成
     *
     * @param jdbcConnectionDetails アプリが使用する接続先の情報（URLのみ使用する）
     * @param user マイグレーション用のユーザー名
     * @param password マイグレーション用のパスワード
     * @return Flywayが使用する接続先の情報
     */
    @Bean
    @Primary
    FlywayConnectionDetails migratorFlywayConnectionDetails(
            JdbcConnectionDetails jdbcConnectionDetails,
            @Value("${spring.flyway.user}") String user,
            @Value("${spring.flyway.password}") String password) {
        return new FlywayConnectionDetails() {

            /**
             * 接続先のURLを返す
             *
             * @return アプリと同じ接続先のURL
             */
            @Override
            public String getJdbcUrl() {
                return jdbcConnectionDetails.getJdbcUrl();
            }

            /**
             * 接続するユーザー名を返す
             *
             * @return マイグレーション用のユーザー名
             */
            @Override
            public String getUsername() {
                return user;
            }

            /**
             * 接続するパスワードを返す
             *
             * @return マイグレーション用のパスワード
             */
            @Override
            public String getPassword() {
                return password;
            }
        };
    }
}
