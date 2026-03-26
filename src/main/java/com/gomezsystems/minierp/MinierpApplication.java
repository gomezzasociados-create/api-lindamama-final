package com.gomezsystems.minierp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // 🔥 Importamos la librería

@SpringBootApplication
@EnableScheduling // 🔥 ENCENDEMOS EL RELOJ AQUÍ
public class MinierpApplication {
	public static void main(String[] args) {
		SpringApplication.run(MinierpApplication.class, args);
	}
}