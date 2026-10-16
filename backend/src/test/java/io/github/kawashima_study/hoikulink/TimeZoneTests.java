package io.github.kawashima_study.hoikulink;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.kawashima_study.hoikulink.shared.BusinessTimeZone;
import java.util.TimeZone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * タイムゾーンの設定のテスト
 */
@DisplayName("タイムゾーンの設定")
class TimeZoneTests {

    /**
     * テストを実行するときの既定のタイムゾーンが日本時間であることを確認
     */
    @Test
    @DisplayName("テストのタイムゾーンが日本時間に固定されている。")
    void defaultTimeZoneIsJapan() {
        assertThat(TimeZone.getDefault().toZoneId()).isEqualTo(BusinessTimeZone.JAPAN);
    }
}
