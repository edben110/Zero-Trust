package com.zerotrust.votingapi.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.votingapi.exception.CanalSeguridadNoDisponibleException;
import com.zerotrust.votingapi.domain.DireccionIp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicadorComandosSeguridadKafkaTest {

    private static final String TOPIC = "security_drills";
    private static final DireccionIp IP = new DireccionIp("203.0.113.10");

    @Mock
    private KafkaTemplate<String, String> kafka;

    private PublicadorComandosSeguridadKafka publicador;

    @BeforeEach
    void preparar() {
        publicador = new PublicadorComandosSeguridadKafka(kafka, new ObjectMapper(), TOPIC, Duration.ofMillis(100));
    }

    private void kafkaConfirma() {
        CompletableFuture<SendResult<String, String>> confirmado = CompletableFuture.completedFuture(null);
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(confirmado);
    }

    @Test
    @DisplayName("Debe publicar EVALUATE con un voto y la IP como clave")
    void publicaEvaluate() {
        // Arrange
        kafkaConfirma();

        // Act
        publicador.evaluar(IP);

        // Assert
        verify(kafka).send(TOPIC, "203.0.113.10", "{\"action\":\"EVALUATE\",\"ip\":\"203.0.113.10\",\"votes\":1}");
    }

    @Test
    @DisplayName("Debe publicar RESET sin votos para la IP")
    void publicaReset() {
        // Arrange
        kafkaConfirma();

        // Act
        publicador.reiniciar(IP);

        // Assert
        verify(kafka).send(TOPIC, "203.0.113.10", "{\"action\":\"RESET\",\"ip\":\"203.0.113.10\",\"votes\":0}");
    }

    @Test
    @DisplayName("Debe publicar RESET_ALL con la clave admin")
    void publicaResetAll() {
        // Arrange
        kafkaConfirma();

        // Act
        publicador.reiniciarTodo();

        // Assert
        verify(kafka).send(TOPIC, "admin", "{\"action\":\"RESET_ALL\"}");
    }

    @Test
    @DisplayName("Debe lanzar CanalSeguridadNoDisponibleException si Kafka rechaza el envío")
    void kafkaRechaza() {
        // Arrange
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("ACL")));

        // Act + Assert
        assertThrows(CanalSeguridadNoDisponibleException.class, () -> publicador.evaluar(IP));
    }

    @Test
    @DisplayName("Debe lanzar CanalSeguridadNoDisponibleException si Kafka no confirma a tiempo")
    void kafkaNoConfirmaATiempo() {
        // Arrange
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(new CompletableFuture<>());

        // Act + Assert
        assertThrows(CanalSeguridadNoDisponibleException.class, () -> publicador.reiniciarTodo());
    }

    @Test
    @DisplayName("Debe exponer el topic de destino")
    void exponeElDestino() {
        // Act + Assert
        assertEquals(TOPIC, publicador.destino());
    }
}
