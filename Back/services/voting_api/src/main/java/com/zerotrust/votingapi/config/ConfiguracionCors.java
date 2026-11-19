package com.zerotrust.votingapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ConfiguracionCors {

    @Bean
    WebMvcConfigurer corsVotacion(Environment entorno) {
        OrigenesCors cors = OrigenesCors.desde(entorno.getProperty("CORS_ALLOWED_ORIGINS"));
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registro) {
                // Las rutas administrativas solo son accesibles desde el BFF con token,
                // nunca se habilitan mediante CORS para navegadores.
                registro.addMapping("/vote")
                        .allowedOrigins(cors.origenes().toArray(String[]::new))
                        .allowedMethods("POST")
                        .allowedHeaders("Content-Type");
                registro.addMapping("/voter-id")
                        .allowedOrigins(cors.origenes().toArray(String[]::new))
                        .allowedMethods("POST")
                        .allowedHeaders("Content-Type");
                registro.addMapping("/catalog")
                        .allowedOrigins(cors.origenes().toArray(String[]::new))
                        .allowedMethods("GET")
                        .allowedHeaders("Content-Type");
            }
        };
    }
}
