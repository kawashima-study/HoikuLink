package io.github.kawashima_study.hoikulink.shared;

import java.time.ZoneId;

/**
 * このシステムの業務のタイムゾーン（日本時間）
 * <p>日付（LocalDate）の判定や表示とログの時刻に使用する
 */
public final class BusinessTimeZone {

    /** 日本時間 */
    public static final ZoneId JAPAN = ZoneId.of("Asia/Tokyo");

    /**
     * 定数のみを持つクラスのためインスタンスを作成させない
     */
    private BusinessTimeZone() {}
}
