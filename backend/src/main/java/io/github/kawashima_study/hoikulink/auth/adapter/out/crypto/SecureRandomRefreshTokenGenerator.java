package io.github.kawashima_study.hoikulink.auth.adapter.out.crypto;

import io.github.kawashima_study.hoikulink.auth.domain.RawRefreshToken;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshTokenGenerator;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * 生のリフレッシュトークンの作成のSecureRandomでの実装
 * <p>32バイトの乱数をBase64URL（パディングなし）の43文字にする
 */
@Component
class SecureRandomRefreshTokenGenerator implements RefreshTokenGenerator {

    /** 乱数のバイト数（256ビット） */
    private static final int TOKEN_BYTES = 32;

    /** 推測できない乱数を作るJava標準の部品（同時に複数の処理から使える） */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 推測できない乱数の文字列で生のリフレッシュトークンを作成
     *
     * @return 新しい生のリフレッシュトークン
     */
    @Override
    public RawRefreshToken generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); // Cookieに入れやすいよう記号は-と_だけにする
        return new RawRefreshToken(value);
    }
}
