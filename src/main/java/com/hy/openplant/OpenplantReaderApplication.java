package com.hy.openplant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.text.SimpleDateFormat;
import java.util.Arrays;

@SpringBootApplication
@EnableScheduling // 开启定时
public class OpenplantReaderApplication {

    public String className = "com.magus.jdbc.Driver";
    // jdbc:产品系列://IP:端口/服务
    // jdbc:产品系列://IP:端口/
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(OpenplantReaderApplication.class, args);
        //输出容器中所有bean
        Arrays.stream(context.getBeanDefinitionNames()).forEach(System.out::println);
    }
}
