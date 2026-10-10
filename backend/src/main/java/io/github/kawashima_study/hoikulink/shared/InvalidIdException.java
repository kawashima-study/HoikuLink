package io.github.kawashima_study.hoikulink.shared;

/**
 * IDの形式が正しくないことを表す。URLの中のIDなら、存在しないIDと同じく404として扱う。
 */
public class InvalidIdException extends IllegalArgumentException {

    public InvalidIdException(String prefix) {
        super("IDの形式が正しくありません。プレフィックス：" + prefix);
    }
}
