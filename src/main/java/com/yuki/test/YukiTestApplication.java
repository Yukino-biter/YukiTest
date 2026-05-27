package com.yuki.test;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.yuki.test.mapper")
@SpringBootApplication
public class YukiTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(YukiTestApplication.class, args);
    }
}
