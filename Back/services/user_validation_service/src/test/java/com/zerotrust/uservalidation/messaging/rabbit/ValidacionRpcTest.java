package com.zerotrust.uservalidation.messaging.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.uservalidation.domain.VoterId;
import com.zerotrust.uservalidation.service.ServicioPadronElectoral;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ValidacionRpcTest {

    private static final String ID = "3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b";

    @Nested
    @DisplayName("LectorPeticionValidacion")
    class Lector {

        private final LectorPeticionValidacion lector = new LectorPeticionValidacion();

        @Test
        @DisplayName("Debe leer el voterId de una petición válida")
        void valida() {
            assertThat(lector.leer(("{\"voterId\":\"" + ID + "\"}").getBytes(StandardCharsets.UTF_8)))
                    .contains(new VoterId(ID));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "no es json", "[]", "{}", "{\"voterId\":\"x\"}", "{\"voterId\":null}"})
        @DisplayName("Una petición malformada no produce voterId")
        void malformada(String cuerpo) {
            assertThat(lector.leer(cuerpo.getBytes(StandardCharsets.UTF_8))).isEmpty();
        }
    }

    @Nested
    @DisplayName("ListenerValidacionRpc")
    @ExtendWith(MockitoExtension.class)
    class Listener {

        @Mock
        private ServicioPadronElectoral servicio;

        private ListenerValidacionRpc listener() {
            return new ListenerValidacionRpc(servicio, new LectorPeticionValidacion(), new ObjectMapper());
        }

        private static Message peticion(String cuerpo) {
            return MessageBuilder.withBody(cuerpo.getBytes(StandardCharsets.UTF_8)).build();
        }

        @Test
        @DisplayName("Debe responder {\"valid\":true} en JSON si el votante es elegible")
        void elegible() {
            when(servicio.esElegible(new VoterId(ID))).thenReturn(true);

            Message respuesta = listener().responder(peticion("{\"voterId\":\"" + ID + "\"}"));

            assertThat(new String(respuesta.getBody(), StandardCharsets.UTF_8)).isEqualTo("{\"valid\":true}");
            assertThat(respuesta.getMessageProperties().getContentType()).isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
            verify(servicio, times(1)).esElegible(new VoterId(ID));
        }

        @Test
        @DisplayName("Debe responder {\"valid\":false} si el votante no es elegible")
        void noElegible() {
            when(servicio.esElegible(new VoterId(ID))).thenReturn(false);

            Message respuesta = listener().responder(peticion("{\"voterId\":\"" + ID + "\"}"));

            assertThat(new String(respuesta.getBody(), StandardCharsets.UTF_8)).isEqualTo("{\"valid\":false}");
        }

        @Test
        @DisplayName("Una petición malformada debe responder {\"valid\":false} sin consultar el padrón")
        void malformada() {
            Message respuesta = listener().responder(peticion("{\"voter\":1}"));

            assertThat(new String(respuesta.getBody(), StandardCharsets.UTF_8)).isEqualTo("{\"valid\":false}");
            verifyNoInteractions(servicio);
        }
    }
}
