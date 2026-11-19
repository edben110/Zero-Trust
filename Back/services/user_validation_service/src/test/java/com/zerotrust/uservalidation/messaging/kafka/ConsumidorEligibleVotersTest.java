package com.zerotrust.uservalidation.messaging.kafka;

import com.zerotrust.uservalidation.domain.VoterId;
import com.zerotrust.uservalidation.service.ServicioPadronElectoral;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class ConsumidorEligibleVotersTest {

    private static final String ID = "3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b";
    private static final String PAYLOAD = "{\"voterId\":\"" + ID
            + "\",\"region\":\"norte\",\"registeredAt\":\"2026-11-14T10:00:00Z\"}";

    @Nested
    @DisplayName("LectorRegistroElegible")
    class Lector {

        private final LectorRegistroElegible lector = new LectorRegistroElegible();

        @Test
        @DisplayName("Un registro con valor es un alta con el voterId de la clave")
        void alta() {
            assertThat(lector.interpretar(ID, PAYLOAD)).contains(new CambioPadron.Alta(new VoterId(ID)));
        }

        @Test
        @DisplayName("Sin clave, el voterId se toma del payload")
        void altaSinClave() {
            assertThat(lector.interpretar(null, PAYLOAD)).contains(new CambioPadron.Alta(new VoterId(ID)));
        }

        @Test
        @DisplayName("Un valor nulo (tombstone) es una baja del voterId de la clave")
        void baja() {
            assertThat(lector.interpretar(ID, null)).contains(new CambioPadron.Baja(new VoterId(ID)));
        }

        @ParameterizedTest
        @ValueSource(strings = {"no es json", "[]", "{}", "{\"voterId\":\"no-uuid\"}", "{\"voterId\":12}"})
        @DisplayName("Un payload malformado se descarta")
        void malformado(String valor) {
            assertThat(lector.interpretar(ID, valor)).isEmpty();
        }

        @Test
        @DisplayName("Clave y payload con voterId distintos se descarta, igual que un tombstone sin clave válida")
        void inconsistente() {
            assertThat(lector.interpretar("9a8b7c6d-5e4f-4a3b-8c2d-1e0f2a3b4c5d", PAYLOAD)).isEmpty();
            assertThat(lector.interpretar(null, null)).isEmpty();
            assertThat(lector.interpretar("no-uuid", null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("ConsumidorEligibleVoters")
    @ExtendWith(MockitoExtension.class)
    class Consumidor {

        @Mock
        private ServicioPadronElectoral servicio;

        private ConsumidorEligibleVoters consumidor() {
            return new ConsumidorEligibleVoters(servicio, new LectorRegistroElegible());
        }

        private static ConsumerRecord<String, String> registro(String clave, String valor) {
            return new ConsumerRecord<>("eligible_voters", 0, 7L, clave, valor);
        }

        @Test
        @DisplayName("Un alta debe registrar al votante exactamente una vez")
        void alta() {
            consumidor().recibir(registro(ID, PAYLOAD));

            verify(servicio, times(1)).registrarElegible(new VoterId(ID));
            verifyNoMoreInteractions(servicio);
        }

        @Test
        @DisplayName("Un tombstone debe retirar al votante")
        void baja() {
            consumidor().recibir(registro(ID, null));

            verify(servicio, times(1)).retirar(new VoterId(ID));
            verifyNoMoreInteractions(servicio);
        }

        @Test
        @DisplayName("Un registro malformado no debe tocar el padrón ni detener el consumo")
        void malformado() {
            consumidor().recibir(registro(ID, "no es json"));

            verifyNoInteractions(servicio);
        }
    }
}
