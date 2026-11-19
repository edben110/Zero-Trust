package com.zerotrust.voterregistration.domain;

import com.zerotrust.voterregistration.exception.PadronInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PadronInicialTest {

    private static final String ID_1 = "3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b";
    private static final String ID_2 = "9a8b7c6d-5e4f-4a3b-8c2d-1e0f2a3b4c5d";
    private static final Instant AHORA = Instant.parse("2026-11-14T08:00:00Z");

    @Nested
    @DisplayName("Value Objects")
    class ValueObjects {

        @ParameterizedTest
        @ValueSource(strings = {"", "x", "1-1-1-1-1"})
        @DisplayName("VoterId debe ser un UUID canónico")
        void voterId(String valor) {
            assertThrows(IllegalArgumentException.class, () -> new VoterId(valor));
            assertThat(new VoterId(ID_1.toUpperCase()).valor()).isEqualTo(ID_1);
        }

        @Test
        @DisplayName("Region no puede estar vacía ni pasar del largo de la tabla")
        void largos() {
            assertThrows(IllegalArgumentException.class, () -> new Region(""));
            assertThrows(IllegalArgumentException.class, () -> new Region("r".repeat(101)));
            assertThrows(IllegalArgumentException.class, () -> new Region(null));
        }
    }

    @Nested
    @DisplayName("PadronInicial")
    class Padron {

        @Test
        @DisplayName("Debe mapear todos los registros con el mismo instante de registro")
        void mapea() {
            // Arrange
            List<RegistroVotanteCrudo> crudos = List.of(
                    new RegistroVotanteCrudo(ID_1, "norte"),
                    new RegistroVotanteCrudo(ID_2, "sur"));

            // Act
            PadronInicial padron = PadronInicial.desde(crudos, AHORA);

            // Assert
            assertThat(padron.votantes()).hasSize(2);
            assertThat(padron.votantes().get(0).voterId()).isEqualTo(new VoterId(ID_1));
            assertThat(padron.votantes()).allMatch(v -> v.registradoEn().equals(AHORA));
        }

        @Test
        @DisplayName("Un registro inválido invalida todo el padrón e indica la posición y el campo")
        void registroInvalido() {
            List<RegistroVotanteCrudo> crudos = List.of(
                    new RegistroVotanteCrudo(ID_1, "norte"),
                    new RegistroVotanteCrudo(ID_2, " "));

            PadronInvalidoException error = assertThrows(PadronInvalidoException.class,
                    () -> PadronInicial.desde(crudos, AHORA));

            assertThat(error.getMessage()).contains("[1]").contains("region");
        }

        @Test
        @DisplayName("Un voterId repetido o un padrón vacío son inválidos")
        void repetidoOVacio() {
            assertThrows(PadronInvalidoException.class, () -> PadronInicial.desde(List.of(
                    new RegistroVotanteCrudo(ID_1, "norte"),
                    new RegistroVotanteCrudo(ID_1.toUpperCase(), "sur")), AHORA));
            assertThrows(PadronInvalidoException.class, () -> PadronInicial.desde(List.of(), AHORA));
        }

        @Test
        @DisplayName("La lista de votantes no se puede modificar desde fuera")
        void inmutable() {
            PadronInicial padron = PadronInicial.desde(List.of(new RegistroVotanteCrudo(ID_1, "norte")), AHORA);

            assertThrows(UnsupportedOperationException.class, () -> padron.votantes().clear());
        }
    }
}
