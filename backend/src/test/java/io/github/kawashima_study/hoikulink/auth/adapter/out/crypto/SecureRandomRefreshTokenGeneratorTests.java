package io.github.kawashima_study.hoikulink.auth.adapter.out.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.kawashima_study.hoikulink.auth.domain.RawRefreshToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 生のリフレッシュトークンの作成（SecureRandomRefreshTokenGenerator）のテスト
 */
@DisplayName("生のリフレッシュトークンの作成")
class SecureRandomRefreshTokenGeneratorTests {

    /** テスト対象 */
    private final SecureRandomRefreshTokenGenerator generator = new SecureRandomRefreshTokenGenerator();

    /**
     * 32バイトの乱数をBase64URLにした43文字になることを確認
     */
    @Test
    @DisplayName("32バイトの乱数をBase64URLにした43文字になる。")
    void generatesBase64UrlOf32Bytes() {
        // 生のリフレッシュトークンを作成
        RawRefreshToken token = generator.generate();

        // Base64URLで使う文字だけの43文字であることを確認
        assertThat(token.value()).matches("[A-Za-z0-9_-]{43}");
    }

    /**
     * 作るたびに違う値になることを確認
     */
    @Test
    @DisplayName("作るたびに違う値になる。")
    void generatesDifferentValueEachTime() {
        // 2回作成
        RawRefreshToken first = generator.generate();
        RawRefreshToken second = generator.generate();

        // 2つの値が違うことを確認
        assertThat(first.value()).isNotEqualTo(second.value());
    }
}
