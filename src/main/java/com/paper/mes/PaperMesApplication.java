package com.paper.mes;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.paper.mes.**.mapper")
public class PaperMesApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaperMesApplication.class, args);
    }
}
