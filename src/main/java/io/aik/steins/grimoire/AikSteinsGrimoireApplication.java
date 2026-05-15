package io.aik.steins.grimoire;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import java.util.TimeZone;

/**
 * AikSteinsGrimoire 启动类
 *
 * @author a I k .
 */
@Slf4j
@SpringBootApplication
public class AikSteinsGrimoireApplication {

    @PostConstruct
    void setDefaultTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
    }

    public static void main(String[] args) {
        SpringApplication.run(AikSteinsGrimoireApplication.class, args);
        log.info("------ AikSteinsGrimoire is successfully started! ------");
    }

}
