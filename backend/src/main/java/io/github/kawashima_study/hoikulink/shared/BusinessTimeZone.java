package io.github.kawashima_study.hoikulink.shared;

import java.time.ZoneId;

/**
 * このシステムの業務のタイムゾーン（日本時間）。
 * 日付（LocalDate）の判定や、表示・ログの時刻に使う。
 */
public final class BusinessTimeZone {

    public static final ZoneId JAPAN = ZoneId.of("Asia/Tokyo");

    private BusinessTimeZone() {
    }

}