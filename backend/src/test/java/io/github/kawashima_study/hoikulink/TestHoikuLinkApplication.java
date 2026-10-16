package io.github.kawashima_study.hoikulink;

import org.springframework.boot.SpringApplication;

/**
 * Testcontainersのコンテナを使ってアプリケーションを手元で起動するためのクラス
 */
public class TestHoikuLinkApplication {

    /**
     * Testcontainersの設定を加えてアプリケーションを起動
     *
     * @param args 起動時の引数
     */
    public static void main(String[] args) {
        SpringApplication.from(HoikuLinkApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
