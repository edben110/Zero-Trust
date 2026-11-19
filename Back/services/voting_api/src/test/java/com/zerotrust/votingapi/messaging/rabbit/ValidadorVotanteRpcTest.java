package com.zerotrust.votingapi.messaging.rabbit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.votingapi.exception.ValidacionNoDisponibleException;
import com.zerotrust.votingapi.domain.VoterId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidadorVotanteRpc")
class ValidadorVotanteRpcTest {

    private static final String VOTANTE = "3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Mock
    private RabbitTemplate plantilla;

    private ValidadorVotanteRpc validador() {
        return new ValidadorVotanteRpc(plantilla, "user_validation_queue", JSON);
    }

    private static Message respuesta(String cuerpo) {
        return MessageBuilder.withBody(cuerpo.getBytes(StandardCharsets.UTF_8)).build();
    }

    @Test
    @DisplayName("Debe enviar {\"voterId\"} a user_validation_queue y leer {\"valid\":true}")
    void habilitado() throws Exception {
        ArgumentCaptor<Message> peticion = ArgumentCaptor.forClass(Message.class);
        when(plantilla.sendAndReceive(eq(""), eq("user_validation_queue"), peticion.capture()))
                .thenReturn(respuesta("{\"valid\":true}"));

        assertThat(validador().estaHabilitado(new VoterId(VOTANTE))).isTrue();

        JsonNode cuerpo = JSON.readTree(peticion.getValue().getBody());
        assertThat(cuerpo.get("voterId").asText()).isEqualTo(VOTANTE);
        assertThat(cuerpo.size()).isEqualTo(1);
        assertThat(peticion.getValue().getMessageProperties().getContentType()).isEqualTo("application/json");
    }

    @Test
    @DisplayName("Debe leer {\"valid\":false}")
    void noHabilitado() {
        when(plantilla.sendAndReceive(anyString(), anyString(), any(Message.class)))
                .thenReturn(respuesta("{\"valid\":false}"));

        assertThat(validador().estaHabilitado(new VoterId(VOTANTE))).isFalse();
    }

    @Test
    @DisplayName("Sin respuesta a tiempo, con el broker caído o con respuesta ilegible, la validación no está disponible")
    void noDisponible() {
        when(plantilla.sendAndReceive(anyString(), anyString(), any(Message.class))).thenReturn(null);
        assertThrows(ValidacionNoDisponibleException.class, () -> validador().estaHabilitado(new VoterId(VOTANTE)));

        when(plantilla.sendAndReceive(anyString(), anyString(), any(Message.class)))
                .thenThrow(new AmqpConnectException(new ConnectException("sin broker")));
        assertThrows(ValidacionNoDisponibleException.class, () -> validador().estaHabilitado(new VoterId(VOTANTE)));
    }

    @Test
    @DisplayName("Una respuesta ilegible o sin booleano no se toma como positiva")
    void respuestaIlegible() {
        for (String cuerpo : new String[] {"no es json", "{}", "{\"valid\":\"true\"}", "\"valido\""}) {
            when(plantilla.sendAndReceive(anyString(), anyString(), any(Message.class))).thenReturn(respuesta(cuerpo));
            assertThrows(ValidacionNoDisponibleException.class, () -> validador().estaHabilitado(new VoterId(VOTANTE)));
        }
    }
}
