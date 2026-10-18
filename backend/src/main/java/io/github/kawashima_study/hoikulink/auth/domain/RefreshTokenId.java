package io.github.kawashima_study.hoikulink.auth.domain;

import io.github.kawashima_study.hoikulink.shared.InvalidIdException;
import io.github.kawashima_study.hoikulink.shared.PrefixedUlid;

/**
 * リフレッシュトークンのID（例 RTK01KAB...）
 *
 * @param value RTKで始まる29文字のID
 */
public record RefreshTokenId(String value) {

    /** リフレッシュトークンIDのプレフィックス */
    private static final String PREFIX = "RTK";

    /**
     * リフレッシュトークンIDを作成
     *
     * @param value RTKで始まる29文字のID
     * @throws NullPointerException 値がnullの場合
     * @throws InvalidIdException 形式が正しくない場合またはプレフィックスがRTKでない場合
     */
    public RefreshTokenId {
        PrefixedUlid.requireValid(PREFIX, value);
    }

    /**
     * 新しいリフレッシュトークンIDを作成
     *
     * @return 新しいリフレッシュトークンID
     */
    public static RefreshTokenId newId() {
        return new RefreshTokenId(PrefixedUlid.generate(PREFIX));
    }

    /**
     * IDの文字列を返す
     * <p>IDは個人の情報ではないためログに出力してよい
     *
     * @return IDの文字列
     */
    @Override
    public String toString() {
        return value;
    }
}
