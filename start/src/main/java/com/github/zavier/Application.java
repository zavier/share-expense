package com.github.zavier;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot Starter
 *
 */
@SpringBootApplication(scanBasePackages = {"com.github.zavier", "com.alibaba.cola"})
@MapperScan(basePackages = {"com.github.zavier.project", "com.github.zavier.user", "com.github.zavier.expense"})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
