package io.github.kawashima_study.hoikulink;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * アプリケーションの起動のテスト
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@DisplayName("アプリケーションの起動")
class HoikuLinkApplicationTests {

    /**
     * Springの部品がすべて作成できアプリケーションが起動することを確認
     */
    @Test
    @DisplayName("アプリケーションが起動する。")
    void contextLoads() {}
}
