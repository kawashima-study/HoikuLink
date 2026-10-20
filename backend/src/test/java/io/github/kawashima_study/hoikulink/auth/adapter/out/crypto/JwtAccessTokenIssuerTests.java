package io.github.kawashima_study.hoikulink.auth.adapter.out.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.github.kawashima_study.hoikulink.auth.domain.AccessToken;
import io.github.kawashima_study.hoikulink.shared.UserId;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * アクセストークンの発行（JwtAccessTokenIssuer）のテスト
 * <p>Springを起動せずにクラスを直接作り発行したJWTをNimbusの部品で読み取って確認する
 */
@DisplayName("アクセストークンの発行（JWT）")
class JwtAccessTokenIssuerTests {

    /** 署名の鍵（32バイトの文字列）テストのデータ */
    private static final byte[] SECRET = "hoikulink-test-jwt-secret-key-32".getBytes(StandardCharsets.UTF_8);

    /** 発行者（テストデータ） */
    private static final String ISSUER = "hoikulink";

    /** 鍵のID（テストデータ） */
    private static final String KEY_ID = "test-key";

    /** 発行日時（テストデータ） */
    private static final Instant ISSUED_AT = Instant.parse("2026-10-20T01:00:00Z");

    /** 有効期限（発行日時の5分後）本体と同じ計算をせず答えの日時をそのまま書く */
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-20T01:05:00Z");

    /** アクセストークンを発行するアカウントのユーザーID（テストデータ） */
    private static final UserId USER_ID = UserId.newId();

    /** テスト対象 */
    private final JwtAccessTokenIssuer issuer =
            new JwtAccessTokenIssuer(Base64.getEncoder().encodeToString(SECRET), ISSUER, KEY_ID);

    /**
     * 設定した鍵で署名されていることを確認
     *
     * @throws Exception JWTの読み取りまたは署名の確認に失敗した場合
     */
    @Test
    @DisplayName("設定した鍵で署名する。")
    void signsWithSecret() throws Exception {
        // アクセストークンを発行して読み取る
        SignedJWT jwt = SignedJWT.parse(issuer.issue(USER_ID, ISSUED_AT).value());

        // 同じ鍵で署名を確認できることを確認
        assertThat(jwt.verify(new MACVerifier(SECRET))).isTrue();
    }

    /**
     * 見出しに署名の方式HS256と鍵のIDが入ることを確認
     *
     * @throws Exception JWTの読み取りに失敗した場合
     */
    @Test
    @DisplayName("見出しに署名の方式HS256と鍵のIDを入れる。")
    void putsAlgorithmAndKeyIdInHeader() throws Exception {
        // アクセストークンを発行して読み取る
        SignedJWT jwt = SignedJWT.parse(issuer.issue(USER_ID, ISSUED_AT).value());

        // 署名の方式と鍵のIDを確認
        assertThat(jwt.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.HS256);
        assertThat(jwt.getHeader().getKeyID()).isEqualTo(KEY_ID);
    }

    /**
     * 中身にユーザーIDと発行者が入ることを確認
     *
     * @throws Exception JWTの読み取りに失敗した場合
     */
    @Test
    @DisplayName("中身にユーザーIDと発行者を入れる。")
    void putsUserIdAndIssuerInClaims() throws Exception {
        // アクセストークンを発行して中身を読み取る
        JWTClaimsSet claims =
                SignedJWT.parse(issuer.issue(USER_ID, ISSUED_AT).value()).getJWTClaimsSet();

        // subがユーザーIDでissが発行者であることを確認
        assertThat(claims.getSubject()).isEqualTo(USER_ID.value());
        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
    }

    /**
     * 有効期限が発行日時の5分後になることを確認
     *
     * @throws Exception JWTの読み取りに失敗した場合
     */
    @Test
    @DisplayName("有効期限は発行日時の5分後になる。")
    void expiresFiveMinutesAfterIssued() throws Exception {
        // アクセストークンを発行して中身を読み取る
        JWTClaimsSet claims =
                SignedJWT.parse(issuer.issue(USER_ID, ISSUED_AT).value()).getJWTClaimsSet();

        // iatが発行日時でexpがその5分後であることを確認
        assertThat(claims.getIssueTime().toInstant()).isEqualTo(ISSUED_AT);
        assertThat(claims.getExpirationTime().toInstant()).isEqualTo(EXPIRES_AT);
    }

    /**
     * 有効期限までの時間が5分であることを確認
     */
    @Test
    @DisplayName("有効期限までの時間は5分になる。")
    void returnsFiveMinutesAsExpiresIn() {
        // アクセストークンを発行
        AccessToken accessToken = issuer.issue(USER_ID, ISSUED_AT);

        // 有効期限までの時間が5分であることを確認
        assertThat(accessToken.expiresIn()).isEqualTo(Duration.ofMinutes(5));
    }

    /**
     * 発行するたびにトークンのIDが変わることを確認
     * <p>ログアウトしたトークンをIDで拒否するため
     *
     * @throws Exception JWTの読み取りに失敗した場合
     */
    @Test
    @DisplayName("発行するたびにトークンのIDが変わる。")
    void usesDifferentIdEachTime() throws Exception {
        // 同じユーザーIDと発行日時で2回発行
        String firstId = SignedJWT.parse(issuer.issue(USER_ID, ISSUED_AT).value())
                .getJWTClaimsSet()
                .getJWTID();
        String secondId = SignedJWT.parse(issuer.issue(USER_ID, ISSUED_AT).value())
                .getJWTClaimsSet()
                .getJWTID();

        // 2つのIDが違うことを確認
        assertThat(firstId).isNotEqualTo(secondId);
    }

    /**
     * 32バイト未満の鍵では作れないことを確認
     */
    @Test
    @DisplayName("32バイト未満の鍵では作れない。")
    void rejectsShortSecret() {
        // 31バイトの鍵を用意
        String shortSecret = Base64.getEncoder().encodeToString(new byte[31]);

        // 作ると例外になることを確認
        assertThatThrownBy(() -> new JwtAccessTokenIssuer(shortSecret, ISSUER, KEY_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
