package io.github.kawashima_study.hoikulink;

import io.github.kawashima_study.hoikulink.shared.BusinessTimeZone;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

/**
 * HoikuLinkのアプリケーションの起動クラス
 * <p>shared は全モジュールから使用できる共有のモジュールとする
 */
@Modulithic(sharedModules = "shared")
@SpringBootApplication
public class HoikuLinkApplication {

    /**
     * アプリケーションを起動
     * <p>実行する環境に関係なく日本時間で動作させるため既定のタイムゾーンを日本時間に固定する
     *
     * @param args 起動時の引数
     */
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(BusinessTimeZone.JAPAN));
        SpringApplication.run(HoikuLinkApplication.class, args);
    }
}
