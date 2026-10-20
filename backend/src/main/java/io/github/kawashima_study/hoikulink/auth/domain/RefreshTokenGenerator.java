package io.github.kawashima_study.hoikulink.auth.domain;

/**
 * 生のリフレッシュトークンを作成するインターフェース
 * <p>乱数の作り方は知らずメソッドの形だけを決める
 * <p>実装はadapterの中に置く
 */
public interface RefreshTokenGenerator {

    /**
     * 推測できない乱数の文字列で生のリフレッシュトークンを作成
     *
     * @return 新しい生のリフレッシュトークン
     */
    RawRefreshToken generate();
}
