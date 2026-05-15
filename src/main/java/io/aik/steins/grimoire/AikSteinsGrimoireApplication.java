package io.aik.steins.grimoire;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
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
@MapperScan(basePackages = "io.aik.steins.grimoire.**.dao", markerInterface = BaseMapper.class)
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
