package com.zerotrust.botdetector.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.botdetector.domain.SecurityAlert;
import com.zerotrust.botdetector.port.FraudIncidentRepository;
import com.zerotrust.botdetector.port.RiskStateRepository;
import com.zerotrust.botdetector.service.RegistroContextosIp;
import com.zerotrust.botdetector.service.ServicioDeteccionBots;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RawVoteKafkaConsumerTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC);
    private static final String IP = "5.5.5.5";

    @Mock
    private KafkaTemplate<String, String> kafka;

    @Mock
    private FraudIncidentRepository incidentes;

    @Mock
    private RiskStateRepository estados;

    private static ConsumerRecord<String, String> voto() {
        return new ConsumerRecord<>("raw_votes", 0, 0L, null, "{\"sourceIp\":\"" + IP + "\"}");
    }

    @Test
    @DisplayName("Debe contar el voto una sola vez y guardar el incidente aunque Kafka rechace la alerta")
    void kafkaCaidoNoProvocaRelecturaNiDobleConteo() {
        // Arrange
        when(kafka.send(eq("security_alerts"), eq(IP), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker caído")));
        SecurityAlertKafkaPublisher publicador =
                new SecurityAlertKafkaPublisher(kafka, new ObjectMapper(), "security_alerts", 3, 100, 0);
        RegistroContextosIp contextos = new RegistroContextosIp(1, 2, 60);
        ServicioDeteccionBots servicio = new ServicioDeteccionBots(publicador, incidentes, estados, contextos, RELOJ);
        RawVoteKafkaConsumer consumidor = new RawVoteKafkaConsumer(servicio, new ObjectMapper());
        consumidor.consume(voto());

        // Act: el segundo voto supera el umbral y dispara la alerta que Kafka rechaza
        assertDoesNotThrow(() -> consumidor.consume(voto()));

        // Assert
        verify(kafka, times(3)).send(eq("security_alerts"), eq(IP), anyString());
        verify(incidentes, times(1)).save(any(SecurityAlert.class));
        assertEquals(2, contextos.votos(IP));
        assertEquals("SOSPECHOSA", contextos.nivelRiesgo(IP));
    }
}
