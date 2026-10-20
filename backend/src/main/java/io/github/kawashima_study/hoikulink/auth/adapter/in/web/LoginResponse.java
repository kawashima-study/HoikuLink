package io.github.kawashima_study.hoikulink.auth.adapter.in.web;

/**
 * ログインAPIの成功の応答の本文
 *
 * @param accessToken アクセストークン（JWT）
 * @param tokenType トークンの種類（Bearer）
 * @param expiresIn アクセストークンの有効期限までの秒数
 */
record LoginResponse(String accessToken, String tokenType, long expiresIn) {

    /**
     * 中身を隠した文字列を返す
     * <p>アクセストークンをログに出力しないため
     *
     * @return 中身を隠した文字列
     */
    @Override
    public String toString() {
        return "LoginResponse[***]";
    }
}
