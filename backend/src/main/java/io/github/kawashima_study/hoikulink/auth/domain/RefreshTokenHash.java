package io.github.kawashima_study.hoikulink.auth.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * リフレッシュトークンのハッシュ値（SHA-256）
 * <p>DBには生のリフレッシュトークンではなくこの値を保存し検索にも使う
 *
 * @param value SHA-256のハッシュ値を16進数の小文字にした文字列（64文字）
 */
public record RefreshTokenHash(String value) {

    /** ハッシュ値の形式（16進数の小文字64文字） */
    private static final Pattern HASH_PATTERN = Pattern.compile("[0-9a-f]{64}");

    /** ハッシュ値の計算方法の名前 */
    private static final String ALGORITHM = "SHA-256";

    /**
     * ハッシュ値を作成
     * <p>DBから読み込んだ値で作る場合に形式を確認する
     *
     * @param value SHA-256のハッシュ値を16進数の小文字にした文字列（64文字）
     * @throws NullPointerException 値がnullの場合
     * @throws IllegalArgumentException 16進数の小文字64文字でない場合
     */
    public RefreshTokenHash {
        Objects.requireNonNull(value, "ハッシュ値がnullです。");
        if (!HASH_PATTERN.matcher(value).matches()) { // 大文字や長さの違う値はプログラムまたは保存データの誤り
            throw new IllegalArgumentException("ハッシュ値の形式が正しくありません。");
        }
    }

    /**
     * 生のリフレッシュトークンからハッシュ値を計算
     *
     * @param rawRefreshToken 生のリフレッシュトークン
     * @return 計算したハッシュ値
     * @throws NullPointerException 生のリフレッシュトークンがnullの場合
     */
    public static RefreshTokenHash of(RawRefreshToken rawRefreshToken) {
        Objects.requireNonNull(rawRefreshToken, "リフレッシュトークンがnullです。");
        byte[] bytes = rawRefreshToken.value().getBytes(StandardCharsets.UTF_8); // 環境によって結果が変わらないよう文字コードを指定する
        byte[] digest = sha256().digest(bytes);
        return new RefreshTokenHash(HexFormat.of().formatHex(digest)); // 16進数の小文字の文字列にしてコンストラクタで形式も確認する
    }

    /**
     * SHA-256の計算の部品を作成
     * <p>MessageDigestは同時に複数の処理から使えないため呼び出すたびに新しく作る
     *
     * @return SHA-256の計算の部品
     * @throws IllegalStateException SHA-256が使えない環境の場合
     */
    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException e) { // SHA-256はJavaの仕様ですべての環境にあるため通常は起きない
            throw new IllegalStateException("SHA-256が使えません。", e);
        }
    }
}
