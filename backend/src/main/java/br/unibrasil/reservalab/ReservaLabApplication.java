package br.unibrasil.reservalab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ReservaLabApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReservaLabApplication.class, args);
    }
}
