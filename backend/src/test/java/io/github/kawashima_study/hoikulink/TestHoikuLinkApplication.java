package io.github.kawashima_study.hoikulink;

import org.springframework.boot.SpringApplication;

public class TestHoikuLinkApplication {

    public static void main(String[] args) {
        SpringApplication.from(HoikuLinkApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
