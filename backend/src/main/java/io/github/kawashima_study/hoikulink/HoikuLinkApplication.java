package io.github.kawashima_study.hoikulink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

@Modulithic(sharedModules = "shared")
@SpringBootApplication
public class HoikuLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(HoikuLinkApplication.class, args);
    }

}
