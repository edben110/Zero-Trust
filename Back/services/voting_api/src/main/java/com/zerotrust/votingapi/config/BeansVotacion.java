package com.zerotrust.votingapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.votingapi.security.DerivadorVoterIdHmac;
import com.zerotrust.votingapi.messaging.kafka.PublicadorElegiblesKafka;
import com.zerotrust.votingapi.messaging.kafka.PublicadorIncidentesFraudeKafka;
import com.zerotrust.votingapi.messaging.kafka.PublicadorVotosKafka;
import com.zerotrust.votingapi.repository.EjecutorTransaccionalSpring;
import com.zerotrust.votingapi.repository.RegistroParticipacionJdbc;
import com.zerotrust.votingapi.repository.RegistroVotoAceptadoJdbc;
import com.zerotrust.votingapi.repository.ControlRegionVotanteJdbc;
import com.zerotrust.votingapi.repository.RepositorioPadronJdbc;
import com.zerotrust.votingapi.messaging.rabbit.ValidadorVotanteRpc;
import com.zerotrust.votingapi.domain.ElectionId;
import com.zerotrust.votingapi.port.DerivadorVoterId;
import com.zerotrust.votingapi.port.EjecutorTransaccional;
import com.zerotrust.votingapi.port.PublicadorElegibles;
import com.zerotrust.votingapi.port.PublicadorVotos;
import com.zerotrust.votingapi.port.RegistroIncidentesFraude;
import com.zerotrust.votingapi.port.RegistroParticipacion;
import com.zerotrust.votingapi.port.RegistroVotoAceptado;
import com.zerotrust.votingapi.port.ControlRegionVotante;
import com.zerotrust.votingapi.port.RepositorioPadron;
import com.zerotrust.votingapi.port.ValidadorVotante;
import com.zerotrust.votingapi.service.ServicioIdentificacion;
import com.zerotrust.votingapi.service.ServicioPadron;
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
    ServicioIdentificacion servicioIdentificacion(DerivadorVoterId derivador, ValidadorVotante validador,
                                                  ElectionId eleccion) {
        return new ServicioIdentificacion(derivador, validador, eleccion);
    }

    @Bean
    RepositorioPadron repositorioPadron(JdbcTemplate jdbc) {
        return new RepositorioPadronJdbc(jdbc);
    }

    @Bean
    PublicadorElegibles publicadorElegibles(KafkaTemplate<String, String> plantilla, ObjectMapper json,
                                            @Value("${votacion.topic-padron}") String topic,
                                            @Value("${votacion.publicacion-timeout-ms}") long esperaMs) {
        return new PublicadorElegiblesKafka(plantilla, topic, json, Duration.ofMillis(esperaMs));
    }

    // Voto duplicado y región incorrecta se publican como incidentes de fraude.
    // bot_detector_service los persiste en fraud_incidents (único escritor de la tabla).
    @Bean
    RegistroIncidentesFraude registroIncidentesFraude(KafkaTemplate<String, String> plantilla, ObjectMapper json,
                                                      @Value("${seguridad.fraud-topic:fraud_events}") String topic) {
        return new PublicadorIncidentesFraudeKafka(plantilla, topic, json);
    }

    // Las regiones válidas son las del catálogo vigente (el que edita el panel de administración).
    @Bean
    ServicioPadron servicioPadron(DerivadorVoterId derivador, RepositorioPadron repositorio,
                                  PublicadorElegibles publicador, EjecutorTransaccional transaccion,
                                  RepositorioCatalogo catalogo, ElectionId eleccion) {
        return new ServicioPadron(derivador, repositorio, publicador, transaccion, catalogo::actual, eleccion,
                Clock.systemUTC());
    }

    @Bean
    RegistroParticipacion registroParticipacion(JdbcTemplate jdbc) {
        return new RegistroParticipacionJdbc(jdbc);
    }

    @Bean
    RegistroVotoAceptado registroVotoAceptado(JdbcTemplate jdbc) {
        return new RegistroVotoAceptadoJdbc(jdbc);
    }

    @Bean
    ControlRegionVotante controlRegionVotante(JdbcTemplate jdbc) {
        return new ControlRegionVotanteJdbc(jdbc);
    }

    @Bean
    EjecutorTransaccional ejecutorTransaccional(PlatformTransactionManager gestor) {
        return new EjecutorTransaccionalSpring(gestor);
    }

    @Bean
    ServicioVotacion servicioVotacion(ValidadorVotante validador, PublicadorVotos publicador,
                                       RegistroParticipacion registro, RegistroVotoAceptado aceptados,
                                       ControlRegionVotante controlRegion, RegistroIncidentesFraude incidentes,
                                       EjecutorTransaccional transaccion,
                                      ElectionId eleccion) {
        return new ServicioVotacion(validador, publicador, registro, aceptados, controlRegion, incidentes, transaccion,
                eleccion, Clock.systemUTC(), UUID::randomUUID);
    }
}
