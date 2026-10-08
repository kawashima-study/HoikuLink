package io.github.kawashima_study.hoikulink;

import io.github.kawashima_study.hoikulink.shared.BusinessTimeZone;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class TimeConfiguration {

    /**
     * 現在の時刻は、すべてこの時計から取る。
     * テストでは、固定した時計や進められる時計に差し替える。
     */
    @Bean
    Clock clock() {
        return Clock.system(BusinessTimeZone.JAPAN);
    }

}