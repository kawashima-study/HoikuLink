package io.github.kawashima_study.hoikulink.shared;

/**
 * IDの形式が正しくないことを表す例外
 * <p>URLの中のIDの場合は存在しないIDと同じく404として扱う
 */
public class InvalidIdException extends IllegalArgumentException {

    /**
     * 例外を作成
     * <p>入力された値はログに不正な文字列が残らないようメッセージに含めない
     *
     * @param prefix 確認したIDのプレフィックス（例 USR）
     */
    public InvalidIdException(String prefix) {
        super("IDの形式が正しくありません（プレフィックス：" + prefix + "）。");
    }
}
