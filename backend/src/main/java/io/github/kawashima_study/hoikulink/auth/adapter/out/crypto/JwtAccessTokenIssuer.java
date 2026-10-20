package io.github.kawashima_study.hoikulink.auth.adapter.out.crypto;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.github.kawashima_study.hoikulink.auth.domain.AccessToken;
import io.github.kawashima_study.hoikulink.auth.domain.AccessTokenIssuer;
import io.github.kawashima_study.hoikulink.shared.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

/**
 * アクセストークン（JWT）の発行のSpring Securityでの実装
 * <p>署名はHS256とし鍵は設定（環境変数HOIKULINK_JWT_SECRET）から受け取る
 */
@Component
class JwtAccessTokenIssuer implements AccessTokenIssuer {

    /** アクセストークンの有効期間（5分） */
    private static final Duration VALIDITY = Duration.ofMinutes(5);

    /** HS256の鍵の最小のバイト数（256ビット） */
    private static final int MIN_SECRET_BYTES = 32;

    /** JWTを作り署名するSpring Securityの部品 */
    private final JwtEncoder jwtEncoder;

    /** 発行者（JWTのiss） */
    private final String issuer;

    /** 鍵のID（JWTの見出しのkid）鍵を切り替えるときにどの鍵で署名したかを区別する */
    private final String keyId;

    /**
     * アクセストークンの発行の部品を作成
     *
     * @param secret Base64で表した署名の鍵（32バイト以上）
     * @param issuer 発行者
     * @param keyId 鍵のID
     * @throws IllegalArgumentException 鍵がBase64の形式でない場合または32バイト未満の場合
     */
    JwtAccessTokenIssuer(
            @Value("${hoikulink.jwt.secret}") String secret, // 設定の値を受け取る（設定がなければ起動しない）
            @Value("${hoikulink.jwt.issuer}") String issuer,
            @Value("${hoikulink.jwt.key-id}") String keyId) {
        this.jwtEncoder = new NimbusJwtEncoder(jwkSource(secret, keyId));
        this.issuer = issuer;
        this.keyId = keyId;
    }

    /**
     * ユーザーIDのアクセストークンを発行
     *
     * @param userId アクセストークンを発行するアカウントのユーザーID
     * @param issuedAt 発行日時
     * @return 発行したアクセストークン
     */
    @Override
    public AccessToken issue(UserId userId, Instant issuedAt) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256) // 署名の方式を見出しに書く
                .keyId(keyId)
                .build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer) // iss
                .subject(userId.value()) // sub（誰のトークンか）
                .issuedAt(issuedAt) // iat
                .expiresAt(issuedAt.plus(VALIDITY)) // exp
                .id(UUID.randomUUID().toString()) // jti（ログアウトしたトークンを拒否するときに使う）
                .build();
        String token =
                jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(token, VALIDITY);
    }

    /**
     * 署名の鍵をSpring Securityの部品が使える形にする
     *
     * @param secret Base64で表した署名の鍵
     * @param keyId 鍵のID
     * @return 署名の鍵の一覧（鍵は1つ）
     * @throws IllegalArgumentException 鍵がBase64の形式でない場合または32バイト未満の場合
     */
    private static JWKSource<SecurityContext> jwkSource(String secret, String keyId) {
        byte[] keyBytes = decode(secret);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("JWTの署名の鍵は32バイト以上にしてください。"); // 鍵の値はメッセージに入れない
        }
        OctetSequenceKey key = new OctetSequenceKey.Builder(keyBytes)
                .keyID(keyId) // 見出しのkidと同じIDを付けないと署名に使う鍵として選ばれない
                .algorithm(JWSAlgorithm.HS256)
                .build();
        return new ImmutableJWKSet<>(new JWKSet(key));
    }

    /**
     * Base64で表した署名の鍵をバイト列に戻す
     *
     * @param secret Base64で表した署名の鍵
     * @return 署名の鍵のバイト列
     * @throws IllegalArgumentException 鍵がBase64の形式でない場合
     */
    private static byte[] decode(String secret) {
        try {
            return Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) { // Java標準のメッセージには鍵の文字が入るため元の例外は付けない
            throw new IllegalArgumentException("JWTの署名の鍵はBase64の形式にしてください。");
        }
    }
}
