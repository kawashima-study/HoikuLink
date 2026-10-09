package io.github.kawashima_study.hoikulink;

import io.github.kawashima_study.hoikulink.shared.BusinessTimeZone;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

@Modulithic(sharedModules = "shared")
@SpringBootApplication
public class HoikuLinkApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(BusinessTimeZone.JAPAN));
        SpringApplication.run(HoikuLinkApplication.class, args);
    }
}
