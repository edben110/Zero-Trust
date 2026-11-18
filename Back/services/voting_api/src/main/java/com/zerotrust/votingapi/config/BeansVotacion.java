package com.zerotrust.votingapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.votingapi.adapter.config.RepositorioCatalogo;
import com.zerotrust.votingapi.adapter.config.OrigenesCors;
import com.zerotrust.votingapi.adapter.config.ResolutorIpOrigen;
import com.zerotrust.votingapi.adapter.crypto.DerivadorVoterIdHmac;
import com.zerotrust.votingapi.adapter.kafka.PublicadorVotosKafka;
import com.zerotrust.votingapi.adapter.persistence.EjecutorTransaccionalSpring;
import com.zerotrust.votingapi.adapter.persistence.RegistroParticipacionJdbc;
import com.zerotrust.votingapi.adapter.rabbit.ValidadorVotanteRpc;
import com.zerotrust.votingapi.domain.ElectionId;
import com.zerotrust.votingapi.port.DerivadorVoterId;
import com.zerotrust.votingapi.port.EjecutorTransaccional;
import com.zerotrust.votingapi.port.PublicadorVotos;
import com.zerotrust.votingapi.port.RegistroParticipacion;
import com.zerotrust.votingapi.port.ValidadorVotante;
import com.zerotrust.votingapi.service.ServicioIdentificacion;
import com.zerotrust.votingapi.service.ServicioVotacion;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

@Configuration
public class BeansVotacion {

    // Fail-fast: sin un catálogo válido el servicio no arranca.
    @Bean
    RepositorioCatalogo catalogo(Environment entorno, ObjectMapper json) {
        String ruta = entorno.getProperty("CATALOG_FILE");
        if (ruta == null || ruta.isBlank()) {
            throw new IllegalStateException("CATALOG_FILE: variable obligatoria sin definir");
        }
        String estado = entorno.getProperty("CATALOG_STATE_FILE");
        if (estado == null || estado.isBlank()) {
            throw new IllegalStateException("CATALOG_STATE_FILE: variable obligatoria sin definir");
        }
        return new RepositorioCatalogo(Path.of(ruta), Path.of(estado), json);
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

    // Fail-fast: sin elección o sin clave HMAC válidas el servicio no arranca. La clave nunca aparece en el mensaje.
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
    ServicioIdentificacion servicioIdentificacion(DerivadorVoterId derivador, ElectionId eleccion) {
        return new ServicioIdentificacion(derivador, eleccion);
    }

    @Bean
    RegistroParticipacion registroParticipacion(JdbcTemplate jdbc) {
        return new RegistroParticipacionJdbc(jdbc);
    }

    @Bean
    EjecutorTransaccional ejecutorTransaccional(PlatformTransactionManager gestor) {
        return new EjecutorTransaccionalSpring(gestor);
    }

    @Bean
    ServicioVotacion servicioVotacion(ValidadorVotante validador, PublicadorVotos publicador,
                                      RegistroParticipacion registro, EjecutorTransaccional transaccion,
                                      ElectionId eleccion) {
        return new ServicioVotacion(validador, publicador, registro, transaccion, eleccion, Clock.systemUTC(),
                UUID::randomUUID);
    }
}
