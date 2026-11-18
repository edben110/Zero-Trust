package com.zerotrust.votingapi.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Cedula y ElectionId")
class CedulaYEleccionTest {

    @Test
    @DisplayName("Debe conservar los ceros iniciales")
    void conservaCeros() {
        assertThat(new Cedula("0012345678").valor()).isEqualTo("0012345678");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567890", " 1234567890 ", "1.234.567.890", "1 234 567 890", "1\u00A0234\u00A0567\u00A0890",
            "\uFF11\uFF12\uFF13\uFF14\uFF15\uFF16\uFF17\uFF18\uFF19\uFF10", "\t1234567890\n"})
    @DisplayName("Entradas equivalentes deben normalizarse al mismo valor")
    void normalizacion(String entrada) {
        assertThat(new Cedula(entrada)).isEqualTo(new Cedula("1234567890"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "12345", "12345678901", "12a456789", "12-3456789",
            "\u0661\u0662\u0663\u0664\u0665\u0666\u0667", "1234567,89", "+1234567890", "1234567890\u0000"})
    @DisplayName("Debe rechazar lo que no son de 6 a 10 dígitos ASCII")
    void rechaza(String entrada) {
        assertThrows(IllegalArgumentException.class, () -> new Cedula(entrada));
    }

    @Test
    @DisplayName("Debe rechazar nulo")
    void rechazaNulo() {
        assertThrows(IllegalArgumentException.class, () -> new Cedula(null));
    }

    @Test
    @DisplayName("Ni toString ni el mensaje de error deben revelar la cédula")
    void noRevela() {
        Cedula cedula = new Cedula("0012345678");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> new Cedula("98765432x1"));

        assertThat(cedula.toString()).doesNotContain("12345678");
        assertThat(error.getMessage()).doesNotContain("98765432");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026", "elecciones-2026", "E.1_x-y", "a"})
    @DisplayName("ElectionId debe aceptar identificadores simples")
    void eleccionValida(String valor) {
        assertThat(new ElectionId(valor).valor()).isEqualTo(valor);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "-x", "a b", "a\u0000b", "ñ", "ab/cd"})
    @DisplayName("ElectionId debe rechazar vacíos y caracteres fuera del formato")
    void eleccionInvalida(String valor) {
        assertThrows(IllegalArgumentException.class, () -> new ElectionId(valor));
    }

    @Test
    @DisplayName("ElectionId debe rechazar nulo y más de 64 caracteres")
    void eleccionLimites() {
        assertThrows(IllegalArgumentException.class, () -> new ElectionId(null));
        assertThrows(IllegalArgumentException.class, () -> new ElectionId("a".repeat(65)));
        assertThat(new ElectionId("a".repeat(64)).valor()).hasSize(64);
    }
}
