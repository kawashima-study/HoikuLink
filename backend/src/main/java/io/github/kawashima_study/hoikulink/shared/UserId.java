package io.github.kawashima_study.hoikulink.shared;

/**
 * アカウントのID（例 USR01KAB...）
 *
 * @param value USR で始まる29文字のID
 */
public record UserId(String value) {

    /** ユーザーIDのプレフィックス */
    private static final String PREFIX = "USR";

    /**
     * ユーザーIDを作成
     *
     * @param value USR で始まる29文字のID
     * @throws NullPointerException 値が null の場合
     * @throws InvalidIdException 形式が正しくない場合またはプレフィックスが USR でない場合
     */
    public UserId {
        PrefixedUlid.requireValid(PREFIX, value);
    }

    /**
     * 新しいユーザーIDを作成
     *
     * @return 新しいユーザーID
     */
    public static UserId newId() {
        return new UserId(PrefixedUlid.generate(PREFIX));
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
