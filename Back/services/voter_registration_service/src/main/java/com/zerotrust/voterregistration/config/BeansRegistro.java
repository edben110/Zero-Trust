package com.zerotrust.voterregistration.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.voterregistration.messaging.kafka.PublicadorElegiblesKafka;
import com.zerotrust.voterregistration.repository.RepositorioPadronJpa;
import com.zerotrust.voterregistration.repository.VotanteJpaRepository;
import com.zerotrust.voterregistration.security.DerivadorVoterIdHmac;
import com.zerotrust.voterregistration.domain.ElectionId;
import com.zerotrust.voterregistration.port.DerivadorVoterId;
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

    // Fail-fast: ELECTION_ID y VOTER_HMAC_KEY deben ser los mismos que usa voting_api, o el padrón no coincidiría.
    @Bean
    ElectionId eleccion(Environment entorno) {
        String valor = entorno.getProperty("ELECTION_ID");
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("ELECTION_ID: variable obligatoria sin definir");
        }
        try {
            return new ElectionId(valor);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("ELECTION_ID: " + e.getMessage());
        }
    }

    @Bean
    DerivadorVoterId derivadorVoterId(Environment entorno) {
        try {
            return DerivadorVoterIdHmac.desdeBase64(entorno.getProperty("VOTER_HMAC_KEY"));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("VOTER_HMAC_KEY: " + e.getMessage());
        }
    }

    @Bean
    FuentePadron fuentePadron(Environment entorno, DerivadorVoterId derivador, ElectionId eleccion) {
        String ruta = entorno.getProperty("VOTERS_FILE");
        if (ruta == null || ruta.isBlank()) {
            throw new IllegalStateException("VOTERS_FILE: variable obligatoria sin definir");
        }
        return new FuentePadronArchivo(Path.of(ruta), new LectorArchivoVotantes(derivador, eleccion));
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
