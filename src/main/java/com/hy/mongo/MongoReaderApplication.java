package com.hy.mongo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // 开启定时
public class MongoReaderApplication {

    public static void main(String[] args) {
        SpringApplication.run(MongoReaderApplication.class, args);
    }

}
