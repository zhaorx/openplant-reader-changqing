package com.hy.http;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // 开启定时
public class HttpReaderApplication {

    public static void main(String[] args) {
        SpringApplication.run(HttpReaderApplication.class, args);
    }

}
