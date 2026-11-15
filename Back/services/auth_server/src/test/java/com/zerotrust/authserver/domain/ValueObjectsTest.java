package com.zerotrust.authserver.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValueObjectsTest {

    private static final String HASH_VALIDO = "$2b$12$" + "a".repeat(53);

    @Nested
    @DisplayName("ClientId")
    class ClientIdTest {

        @Test
        @DisplayName("Debe aceptar un client_id sin espacios y distinguir mayúsculas")
        void aceptaYDistingueMayusculas() {
            // Arrange
            ClientId minusculas = new ClientId("voting_api");

            // Act
            ClientId mayusculas = new ClientId("VOTING_API");

            // Assert
            assertThat(minusculas).isNotEqualTo(mayusculas);
            assertThat(minusculas.valor()).isEqualTo("voting_api");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "a b", "a\tb", "a b"})
        @DisplayName("Debe rechazar un client_id vacío o con espacios en blanco")
        void rechazaVacioOConEspacios(String valor) {
            assertThrows(IllegalArgumentException.class, () -> new ClientId(valor));
        }

        @Test
        @DisplayName("Debe rechazar un client_id nulo")
        void rechazaNulo() {
            assertThrows(IllegalArgumentException.class, () -> new ClientId(null));
        }

        @Test
        @DisplayName("intentar debe devolver vacío sin lanzar excepción si el formato no es válido")
        void intentarDevuelveVacio() {
            assertThat(ClientId.intentar("a b")).isEmpty();
            assertThat(ClientId.intentar(null)).isEmpty();
            assertThat(ClientId.intentar("voting_api")).contains(new ClientId("voting_api"));
        }
    }

    @Nested
    @DisplayName("SecretoCliente")
    class SecretoClienteTest {

        @Test
        @DisplayName("Debe enmascarar el secreto en toString")
        void enmascara() {
            assertThat(new SecretoCliente("muy-secreto").toString()).doesNotContain("muy-secreto");
        }

        @Test
        @DisplayName("Debe detectar un secreto de más de 72 bytes en UTF-8")
        void detectaLimiteBcrypt() {
            assertThat(new SecretoCliente("a".repeat(72)).excedeLimiteBcrypt()).isFalse();
            assertThat(new SecretoCliente("a".repeat(73)).excedeLimiteBcrypt()).isTrue();
            assertThat(new SecretoCliente("ñ".repeat(37)).excedeLimiteBcrypt()).isTrue();
        }

        @Test
        @DisplayName("Debe rechazar un secreto nulo o vacío")
        void rechazaVacio() {
            assertThrows(IllegalArgumentException.class, () -> new SecretoCliente(null));
            assertThrows(IllegalArgumentException.class, () -> new SecretoCliente(""));
        }
    }

    @Nested
    @DisplayName("HashSecreto")
    class HashSecretoTest {

        @ParameterizedTest
        @ValueSource(strings = {"$2a$10$", "$2b$12$", "$2y$14$"})
        @DisplayName("Debe aceptar los prefijos y costes válidos")
        void aceptaFormatoValido(String prefijo) {
            HashSecreto hash = new HashSecreto(prefijo + "./AZaz09".repeat(6) + "abcde");
            assertThat(hash.coste()).isBetween(10, 14);
        }

        @Test
        @DisplayName("Debe extraer el coste")
        void extraeCoste() {
            assertThat(new HashSecreto(HASH_VALIDO).coste()).isEqualTo(12);
        }

        @ParameterizedTest
        @ValueSource(strings = {"$2b$09$", "$2b$15$", "$2x$12$", "$2b$1a$", "x2b$12$"})
        @DisplayName("Debe rechazar prefijos o costes fuera de rango")
        void rechazaPrefijoOCoste(String prefijo) {
            String hash = prefijo + "a".repeat(53);
            assertThrows(IllegalArgumentException.class, () -> new HashSecreto(hash));
        }

        @Test
        @DisplayName("Debe exigir exactamente 60 caracteres")
        void exigeLongitud() {
            assertThrows(IllegalArgumentException.class, () -> new HashSecreto("$2b$12$" + "a".repeat(52)));
            assertThrows(IllegalArgumentException.class, () -> new HashSecreto("$2b$12$" + "a".repeat(54)));
            assertThat(new HashSecreto("$2b$12$" + "a".repeat(53)).valor()).hasSize(60);
        }

        @Test
        @DisplayName("Debe rechazar caracteres fuera del alfabeto BCrypt")
        void rechazaAlfabeto() {
            assertThrows(IllegalArgumentException.class, () -> new HashSecreto("$2b$12$" + "a".repeat(52) + "+"));
        }

        @Test
        @DisplayName("Debe enmascarar el hash en toString")
        void enmascara() {
            assertThat(new HashSecreto(HASH_VALIDO).toString()).doesNotContain("aaaa");
        }
    }

    @Nested
    @DisplayName("Scope")
    class ScopeTest {

        @ParameterizedTest
        @ValueSource(strings = {"rabbitmq.read:votos/cola", "a", "!#[]~"})
        @DisplayName("Debe aceptar scopes que cumplen la gramática scope-token")
        void aceptaValidos(String valor) {
            assertThat(new Scope(valor).valor()).isEqualTo(valor);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "a b", "a\"b", "a\\b", "a\tb", "ñ"})
        @DisplayName("Debe rechazar scopes que no cumplen la gramática scope-token")
        void rechazaInvalidos(String valor) {
            assertThrows(IllegalArgumentException.class, () -> new Scope(valor));
        }
    }

    @Nested
    @DisplayName("Audiencia")
    class AudienciaTest {

        @Test
        @DisplayName("Debe convertir kafka y rabbitmq y rechazar otros valores")
        void convierte() {
            assertThat(Audiencia.desde("kafka")).isEqualTo(Audiencia.KAFKA);
            assertThat(Audiencia.desde("rabbitmq")).isEqualTo(Audiencia.RABBITMQ);
            assertThat(Audiencia.RABBITMQ.valor()).isEqualTo("rabbitmq");
            assertThrows(IllegalArgumentException.class, () -> Audiencia.desde("Kafka"));
            assertThrows(IllegalArgumentException.class, () -> Audiencia.desde("postgres"));
        }
    }

    @Nested
    @DisplayName("Ttl")
    class TtlTest {

        @ParameterizedTest
        @ValueSource(strings = {"60", "300", "3600"})
        @DisplayName("Debe aceptar TTL entre 60 y 3600")
        void aceptaRango(String texto) {
            assertThat(Ttl.desde(texto).segundos()).isEqualTo(Integer.parseInt(texto));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "0", "59", "3601", "-300", "1.5", "abc", " 300"})
        @DisplayName("Debe rechazar TTL vacío, fuera de rango o no entero")
        void rechazaInvalidos(String texto) {
            assertThrows(IllegalArgumentException.class, () -> Ttl.desde(texto));
        }

        @Test
        @DisplayName("El TTL por defecto debe ser 300")
        void porDefecto() {
            assertThat(Ttl.porDefecto().segundos()).isEqualTo(300);
        }
    }

    @Nested
    @DisplayName("Emisor")
    class EmisorTest {

        @Test
        @DisplayName("Debe conservar el valor literal, incluida la barra final")
        void conservaLiteral() {
            assertThat(new Emisor("https://vm-auth/").valor()).isEqualTo("https://vm-auth/");
            assertThat(new Emisor("https://vm-auth").valor()).isEqualTo("https://vm-auth");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "http://vm-auth", "vm-auth", "/token", "https:foo", "https://", "ftp://vm-auth"})
        @DisplayName("Debe exigir una URL absoluta https con host")
        void rechazaInvalidos(String valor) {
            assertThrows(IllegalArgumentException.class, () -> new Emisor(valor));
        }
    }

    @Nested
    @DisplayName("IdClave")
    class IdClaveTest {

        @Test
        @DisplayName("Debe rechazar un kid vacío")
        void rechazaVacio() {
            assertThrows(IllegalArgumentException.class, () -> new IdClave(""));
            assertThrows(IllegalArgumentException.class, () -> new IdClave("  "));
            assertThrows(IllegalArgumentException.class, () -> new IdClave(null));
            assertThat(new IdClave("clave-1").valor()).isEqualTo("clave-1");
        }
    }

    @Nested
    @DisplayName("PuertoServidor")
    class PuertoServidorTest {

        @ParameterizedTest
        @ValueSource(strings = {"1024", "8443", "65535"})
        @DisplayName("Debe aceptar puertos entre 1024 y 65535")
        void aceptaRango(String texto) {
            assertThat(PuertoServidor.desde(texto).valor()).isEqualTo(Integer.parseInt(texto));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "1023", "65536", "-1", "80a", "8443.0"})
        @DisplayName("Debe rechazar puertos fuera de rango o no enteros")
        void rechazaInvalidos(String texto) {
            assertThrows(IllegalArgumentException.class, () -> PuertoServidor.desde(texto));
        }

        @Test
        @DisplayName("El puerto por defecto debe ser 8443")
        void porDefecto() {
            assertThat(PuertoServidor.porDefecto().valor()).isEqualTo(8443);
        }
    }
}
