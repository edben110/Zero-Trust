package com.zerotrust.botdetector.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Reloj inyectable: la ventana de detección debe poder probarse con tiempo fijo
 * sin dormir el hilo ni depender del reloj del sistema.
 */
@Configuration
public class RelojConfig {

    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }
}
