package com.zerotrust.uservalidation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PadronElectoralTest {

    private static final String UUID_1 = "3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b";
    private static final String UUID_2 = "9a8b7c6d-5e4f-4a3b-8c2d-1e0f2a3b4c5d";

    @Nested
    @DisplayName("VoterId")
    class VoterIdTest {

        @Test
        @DisplayName("Debe aceptar un UUID canónico y normalizarlo a minúsculas")
        void aceptaUuid() {
            assertThat(new VoterId(UUID_1.toUpperCase()).valor()).isEqualTo(UUID_1);
            assertThat(new VoterId(UUID_1)).isEqualTo(new VoterId(UUID_1.toUpperCase()));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "no-es-uuid", "1-1-1-1-1", "3f2b8c1e6d4a4b7e9c210a5d6e7f8a9b",
                "3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9", "3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9bz"})
        @DisplayName("Debe rechazar valores que no son un UUID canónico")
        void rechazaInvalidos(String valor) {
            assertThrows(IllegalArgumentException.class, () -> new VoterId(valor));
        }

        @Test
        @DisplayName("Debe rechazar un valor nulo")
        void rechazaNulo() {
            assertThrows(IllegalArgumentException.class, () -> new VoterId(null));
        }
    }

    @Nested
    @DisplayName("PadronElectoral")
    class Padron {

        @Test
        @DisplayName("Un votante habilitado debe constar como elegible y uno desconocido no")
        void altaYConsulta() {
            // Arrange
            PadronElectoral padron = new PadronElectoral();

            // Act
            padron.habilitar(new VoterId(UUID_1));

            // Assert
            assertThat(padron.estaHabilitado(new VoterId(UUID_1))).isTrue();
            assertThat(padron.estaHabilitado(new VoterId(UUID_2))).isFalse();
            assertThat(padron.total()).isEqualTo(1);
        }

        @Test
        @DisplayName("Habilitar dos veces no duplica y retirar elimina al votante")
        void bajaEIdempotencia() {
            PadronElectoral padron = new PadronElectoral();
            padron.habilitar(new VoterId(UUID_1));
            padron.habilitar(new VoterId(UUID_1));

            padron.retirar(new VoterId(UUID_1));

            assertThat(padron.estaHabilitado(new VoterId(UUID_1))).isFalse();
            assertThat(padron.total()).isZero();
        }

        @Test
        @DisplayName("Retirar a un votante que no está no debe fallar")
        void retirarInexistente() {
            PadronElectoral padron = new PadronElectoral();

            padron.retirar(new VoterId(UUID_2));

            assertThat(padron.total()).isZero();
        }

        @Test
        @DisplayName("Debe admitir altas y consultas concurrentes")
        void concurrente() throws Exception {
            PadronElectoral padron = new PadronElectoral();
            ExecutorService hilos = Executors.newFixedThreadPool(8);
            CountDownLatch fin = new CountDownLatch(1000);
            for (int i = 0; i < 1000; i++) {
                String id = String.format("00000000-0000-4000-8000-%012d", i);
                hilos.submit(() -> {
                    padron.habilitar(new VoterId(id));
                    padron.estaHabilitado(new VoterId(id));
                    fin.countDown();
                });
            }

            assertThat(fin.await(10, TimeUnit.SECONDS)).isTrue();
            hilos.shutdown();
            assertThat(padron.total()).isEqualTo(1000);
        }
    }
}
