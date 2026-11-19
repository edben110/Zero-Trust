package com.zerotrust.votingapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.votingapi.adapter.kafka.PublicadorComandosSeguridadKafka;
import com.zerotrust.votingapi.adapter.persistence.ConsultaSeguridadJdbc;
import com.zerotrust.votingapi.domain.UmbralesSeguridad;
import com.zerotrust.votingapi.port.ConsultaSeguridad;
import com.zerotrust.votingapi.port.PublicadorComandosSeguridad;
import com.zerotrust.votingapi.service.ServicioSeguridadAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;

/**
 * Beans del panel de seguridad (pruebas controladas, reinicios y purga del detector).
 */
@Configuration
public class BeansSeguridad {

    @Bean
    UmbralesSeguridad umbralesSeguridad(@Value("${seguridad.suspicious-threshold:5}") int sospechosa,
                                        @Value("${seguridad.blocked-threshold:10}") int bloqueada,
                                        @Value("${seguridad.window-seconds:60}") int ventanaSegundos) {
        return new UmbralesSeguridad(sospechosa, bloqueada, ventanaSegundos);
    }

    @Bean
    ConsultaSeguridad consultaSeguridad(JdbcTemplate jdbc) {
        return new ConsultaSeguridadJdbc(jdbc);
    }

    @Bean
    PublicadorComandosSeguridad publicadorComandosSeguridad(KafkaTemplate<String, String> kafka, ObjectMapper json,
                                                            @Value("${seguridad.drills-topic:security_drills}") String topic,
                                                            @Value("${seguridad.espera-kafka-ms:5000}") long esperaMs) {
        return new PublicadorComandosSeguridadKafka(kafka, json, topic, Duration.ofMillis(esperaMs));
    }

    @Bean
    ServicioSeguridadAdmin servicioSeguridadAdmin(ConsultaSeguridad consulta, PublicadorComandosSeguridad publicador,
                                                  UmbralesSeguridad umbrales) {
        return new ServicioSeguridadAdmin(consulta, publicador, umbrales);
    }
}
