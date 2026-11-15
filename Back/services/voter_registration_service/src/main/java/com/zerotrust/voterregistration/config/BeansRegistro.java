package com.zerotrust.voterregistration.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.voterregistration.adapter.archivo.FuentePadronArchivo;
import com.zerotrust.voterregistration.adapter.archivo.LectorArchivoVotantes;
import com.zerotrust.voterregistration.adapter.kafka.PublicadorElegiblesKafka;
import com.zerotrust.voterregistration.adapter.persistence.RepositorioPadronJpa;
import com.zerotrust.voterregistration.adapter.persistence.VotanteJpaRepository;
import com.zerotrust.voterregistration.port.FuentePadron;
import com.zerotrust.voterregistration.port.PublicadorElegibles;
import com.zerotrust.voterregistration.port.RepositorioPadron;
import com.zerotrust.voterregistration.service.ServicioRegistroPadron;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;

@Configuration
public class BeansRegistro {

    @Bean
    FuentePadron fuentePadron(Environment entorno) {
        String ruta = entorno.getProperty("VOTERS_FILE");
        if (ruta == null || ruta.isBlank()) {
            throw new IllegalStateException("VOTERS_FILE: variable obligatoria sin definir");
        }
        return new FuentePadronArchivo(Path.of(ruta), new LectorArchivoVotantes());
    }

    @Bean
    RepositorioPadron repositorioPadron(VotanteJpaRepository jpa) {
        return new RepositorioPadronJpa(jpa);
    }

    @Bean
    PublicadorElegibles publicadorElegibles(KafkaTemplate<String, String> plantilla,
                                            @Value("${registro.topic}") String topic,
                                            @Value("${registro.publicacion-timeout-ms}") long esperaMs) {
        return new PublicadorElegiblesKafka(plantilla, topic, new ObjectMapper(), Duration.ofMillis(esperaMs));
    }

    @Bean
    ServicioRegistroPadron servicioRegistroPadron(FuentePadron fuente, RepositorioPadron repositorio,
                                                  PublicadorElegibles publicador) {
        return new ServicioRegistroPadron(fuente, repositorio, publicador, Clock.systemUTC());
    }

    @Bean
    TareaRegistroPadron tareaRegistroPadron(ServicioRegistroPadron servicio) {
        return new TareaRegistroPadron(servicio);
    }
}
