package io.github.kawashima_study.hoikulink;

import io.github.kawashima_study.hoikulink.shared.BusinessTimeZone;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 時刻に関する設定
 */
@Configuration(proxyBeanMethods = false)
class TimeConfiguration {

    /**
     * 現在の時刻を取得する時計を作成
     * <p>現在の時刻はすべてこの時計から取得する
     * <p>テストでは固定した時計や進められる時計に差し替える
     *
     * @return 日本時間の時計
     */
    @Bean
    Clock clock() {
        return Clock.system(BusinessTimeZone.JAPAN);
    }
}
