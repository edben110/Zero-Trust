package com.zerotrust.authserver.adapter.config;

import com.zerotrust.authserver.domain.Audiencia;
import com.zerotrust.authserver.domain.ClientId;
import com.zerotrust.authserver.domain.ConfiguracionInvalidaException;
import com.zerotrust.authserver.domain.RegistroClientes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InterpreteArchivoClientesTest {

    private static final String HASH = "$2b$12$" + "a".repeat(53);
    private static final String HASH_10 = "$2b$10$" + "b".repeat(53);

    private final InterpreteArchivoClientes interprete = new InterpreteArchivoClientes();

    private static String cliente(String id, String hash, String scopes, String audiences) {
        return "{\"client_id\":\"" + id + "\",\"secret_hash\":\"" + hash + "\",\"scopes\":" + scopes
                + ",\"audiences\":" + audiences + "}";
    }

    private static String archivo(String... clientes) {
        return "{\"clients\":[" + String.join(",", clientes) + "]}";
    }

    private RegistroClientes interpretar(String json) {
        return interprete.interpretar(json.getBytes(StandardCharsets.UTF_8));
    }

    private ConfiguracionInvalidaException falla(String json) {
        ConfiguracionInvalidaException error = assertThrows(ConfiguracionInvalidaException.class, () -> interpretar(json));
        assertThat(error.getMessage()).doesNotContain(HASH).doesNotContain("aaaa").doesNotContain("Jackson")
                .doesNotContain("line:").doesNotContain("column:");
        assertThat(error.getCause()).isNull();
        return error;
    }

    @Test
    @DisplayName("Debe construir el registro a partir de un archivo válido")
    void archivoValido() {
        // Arrange
        String json = archivo(
                cliente("voting_api", HASH, "[\"rabbitmq.write:votos/cola\"]", "[\"rabbitmq\",\"kafka\"]"),
                cliente("analytics_archiver", HASH, "[]", "[\"kafka\"]"));

        // Act
        RegistroClientes registro = interpretar(json);

        // Assert
        assertThat(registro.buscar(new ClientId("voting_api")).orElseThrow().audiencias())
                .containsExactly(Audiencia.RABBITMQ, Audiencia.KAFKA);
        assertThat(registro.buscar(new ClientId("analytics_archiver"))).isPresent();
        assertThat(registro.costeComun()).isEqualTo(12);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "no es json", "{\"clients\":[", "[]", "{}", "{\"clients\":{}}", "{\"clients\":[]}",
            "{\"clients\":[\"texto\"]}"})
    @DisplayName("Debe rechazar archivos que no son JSON válido o con raíz incorrecta")
    void rechazaRaizInvalida(String json) {
        falla(json);
    }

    @Test
    @DisplayName("Debe rechazar bytes UTF-8 inválidos")
    void rechazaUtf8Invalido() {
        byte[] bytes = {'{', '"', (byte) 0xC3, (byte) 0x28, '"', ':', '1', '}'};
        assertThrows(ConfiguracionInvalidaException.class, () -> interprete.interpretar(bytes));
    }

    @Test
    @DisplayName("Debe rechazar claves JSON duplicadas")
    void rechazaClavesDuplicadas() {
        String json = "{\"clients\":[{\"client_id\":\"a\",\"client_id\":\"b\",\"secret_hash\":\"" + HASH
                + "\",\"scopes\":[\"x\"],\"audiences\":[\"rabbitmq\"]}]}";
        assertThat(falla(json).getMessage()).contains("duplicad");
    }

    @Test
    @DisplayName("Debe rechazar campos desconocidos en la raíz y en un cliente")
    void rechazaCamposDesconocidos() {
        falla("{\"clients\":[" + cliente("a", HASH, "[\"x\"]", "[\"rabbitmq\"]") + "],\"extra\":1}");
        falla(archivo("{\"client_id\":\"a\",\"secret_hash\":\"" + HASH
                + "\",\"scopes\":[\"x\"],\"audiences\":[\"rabbitmq\"],\"extra\":true}"));
    }

    @Test
    @DisplayName("Debe rechazar client_id ausente, vacío, con espacios o repetido")
    void rechazaClientIdInvalido() {
        falla(archivo("{\"secret_hash\":\"" + HASH + "\",\"scopes\":[\"x\"],\"audiences\":[\"rabbitmq\"]}"));
        falla(archivo(cliente("", HASH, "[\"x\"]", "[\"rabbitmq\"]")));
        falla(archivo(cliente("a b", HASH, "[\"x\"]", "[\"rabbitmq\"]")));
        falla(archivo(cliente("a", HASH, "[\"x\"]", "[\"rabbitmq\"]"), cliente("a", HASH, "[\"y\"]", "[\"rabbitmq\"]")));
    }

    @Test
    @DisplayName("Debe rechazar secret_hash ausente, mal formado o con costes distintos")
    void rechazaHashInvalido() {
        falla(archivo("{\"client_id\":\"a\",\"scopes\":[\"x\"],\"audiences\":[\"rabbitmq\"]}"));
        falla(archivo(cliente("a", "$2b$09$" + "a".repeat(53), "[\"x\"]", "[\"rabbitmq\"]")));
        falla(archivo(cliente("a", HASH, "[\"x\"]", "[\"rabbitmq\"]"), cliente("b", HASH_10, "[\"y\"]", "[\"rabbitmq\"]")));
    }

    @Test
    @DisplayName("Debe rechazar scopes ausente, vacía sin ser solo kafka, repetidos o fuera de la gramática")
    void rechazaScopesInvalidos() {
        falla(archivo("{\"client_id\":\"a\",\"secret_hash\":\"" + HASH + "\",\"audiences\":[\"kafka\"]}"));
        falla(archivo(cliente("a", HASH, "[]", "[\"rabbitmq\"]")));
        falla(archivo(cliente("a", HASH, "[]", "[\"kafka\",\"rabbitmq\"]")));
        falla(archivo(cliente("a", HASH, "[\"x\",\"x\"]", "[\"rabbitmq\"]")));
        falla(archivo(cliente("a", HASH, "[\"\"]", "[\"rabbitmq\"]")));
        falla(archivo(cliente("a", HASH, "[\"a b\"]", "[\"rabbitmq\"]")));
        falla(archivo(cliente("a", HASH, "[1]", "[\"rabbitmq\"]")));
    }

    @Test
    @DisplayName("Debe rechazar audiences ausente, vacía, desconocida o repetida")
    void rechazaAudiencesInvalidas() {
        falla(archivo("{\"client_id\":\"a\",\"secret_hash\":\"" + HASH + "\",\"scopes\":[\"x\"]}"));
        falla(archivo(cliente("a", HASH, "[\"x\"]", "[]")));
        falla(archivo(cliente("a", HASH, "[\"x\"]", "[\"postgres\"]")));
        falla(archivo(cliente("a", HASH, "[\"x\"]", "[\"rabbitmq\",\"rabbitmq\"]")));
    }

    @Test
    @DisplayName("El mensaje debe indicar la posición del cliente y el campo, sin el valor del hash")
    void mensajeConPosicion() {
        String mensaje = falla(archivo(cliente("a", HASH, "[\"x\"]", "[\"rabbitmq\"]"),
                cliente("b", "$2b$12$corto", "[\"x\"]", "[\"rabbitmq\"]"))).getMessage();

        assertThat(mensaje).contains("clients[1]").contains("secret_hash").doesNotContain("corto");
    }
}
