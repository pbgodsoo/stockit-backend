package org.example.stockitbe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StockitBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockitBeApplication.class, args);
    }

}
