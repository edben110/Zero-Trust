package com.zerotrust.votingapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.votingapi.adapter.config.LectorCatalogo;
import com.zerotrust.votingapi.adapter.config.OrigenesCors;
import com.zerotrust.votingapi.adapter.config.ResolutorIpOrigen;
import com.zerotrust.votingapi.adapter.kafka.PublicadorVotosKafka;
import com.zerotrust.votingapi.adapter.rabbit.ValidadorVotanteRpc;
import com.zerotrust.votingapi.domain.Catalogo;
import com.zerotrust.votingapi.port.PublicadorVotos;
import com.zerotrust.votingapi.port.ValidadorVotante;
import com.zerotrust.votingapi.service.ServicioVotacion;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

@Configuration
public class BeansVotacion {

    // Fail-fast: sin un catálogo válido el servicio no arranca.
    @Bean
    Catalogo catalogo(Environment entorno) {
        String ruta = entorno.getProperty("CATALOG_FILE");
        if (ruta == null || ruta.isBlank()) {
            throw new IllegalStateException("CATALOG_FILE: variable obligatoria sin definir");
        }
        try {
            return new LectorCatalogo().interpretar(Files.readAllBytes(Path.of(ruta)));
        } catch (IOException e) {
            throw new IllegalStateException("CATALOG_FILE: no se puede leer el archivo indicado");
        }
    }

    @Bean
    ResolutorIpOrigen resolutorIpOrigen(Environment entorno) {
        return ResolutorIpOrigen.desde(entorno.getProperty("TRUST_FORWARDED_HEADERS"));
    }

    @Bean
    WebMvcConfigurer corsVotacion(Environment entorno) {
        OrigenesCors cors = OrigenesCors.desde(entorno.getProperty("CORS_ALLOWED_ORIGINS"));
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registro) {
                registro.addMapping("/**")
                        .allowedOrigins(cors.origenes().toArray(String[]::new))
                        .allowedMethods("GET", "POST")
                        .allowedHeaders("Content-Type");
            }
        };
    }

    @Bean
    ValidadorVotante validadorVotante(RabbitTemplate plantilla, ObjectMapper json,
                                      @Value("${votacion.cola-validacion}") String cola) {
        return new ValidadorVotanteRpc(plantilla, cola, json);
    }

    @Bean
    PublicadorVotos publicadorVotos(KafkaTemplate<String, String> plantilla, ObjectMapper json,
                                    @Value("${votacion.topic}") String topic,
                                    @Value("${votacion.publicacion-timeout-ms}") long esperaMs) {
        return new PublicadorVotosKafka(plantilla, topic, json, Duration.ofMillis(esperaMs));
    }

    @Bean
    ServicioVotacion servicioVotacion(ValidadorVotante validador, PublicadorVotos publicador) {
        return new ServicioVotacion(validador, publicador, Clock.systemUTC(), UUID::randomUUID);
    }
}
