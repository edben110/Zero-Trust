package com.zerotrust.authserver.adapter.web;

import com.zerotrust.authserver.domain.PeticionMalFormadaException;
import com.zerotrust.authserver.service.PeticionToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InterpretePeticionTokenTest {

    private static final String FORM = "application/x-www-form-urlencoded";
    private final InterpretePeticionToken interprete = new InterpretePeticionToken();

    private static byte[] cuerpo(String texto) {
        return texto.getBytes(StandardCharsets.UTF_8);
    }

    private static String basic(String credenciales) {
        return "Basic " + Base64.getEncoder().encodeToString(credenciales.getBytes(StandardCharsets.UTF_8));
    }

    private PeticionToken interpretar(String contentType, List<String> autorizaciones, String texto) {
        return interprete.interpretar(contentType, autorizaciones, cuerpo(texto));
    }

    private PeticionMalFormadaException rechaza(String contentType, List<String> autorizaciones, byte[] contenido) {
        return assertThrows(PeticionMalFormadaException.class,
                () -> interprete.interpretar(contentType, autorizaciones, contenido));
    }

    @Nested
    @DisplayName("Peticiones válidas")
    class Validas {

        @Test
        @DisplayName("Debe leer las credenciales del cuerpo")
        void credencialesEnCuerpo() {
            // Arrange
            String texto = "grant_type=client_credentials&client_id=voting_api&client_secret=s3cr3t&scope=a+b";

            // Act
            PeticionToken peticion = interpretar(FORM, List.of(), texto);

            // Assert
            assertThat(peticion.grantType()).isEqualTo("client_credentials");
            assertThat(peticion.clientIdCrudo()).isEqualTo("voting_api");
            assertThat(peticion.secreto().valor()).isEqualTo("s3cr3t");
            assertThat(peticion.scopeCrudo()).isEqualTo("a b");
            assertThat(peticion.porBasic()).isFalse();
        }

        @Test
        @DisplayName("Debe leer las credenciales por Basic con el esquema sin distinguir mayúsculas")
        void credencialesPorBasic() {
            for (String esquema : List.of("Basic ", "basic ", "BASIC ")) {
                String cabecera = esquema + basic("voting_api:s3cr3t").substring("Basic ".length());

                PeticionToken peticion = interpretar(FORM, List.of(cabecera), "grant_type=client_credentials");

                assertThat(peticion.clientIdCrudo()).isEqualTo("voting_api");
                assertThat(peticion.secreto().valor()).isEqualTo("s3cr3t");
                assertThat(peticion.porBasic()).isTrue();
                assertThat(peticion.scopeCrudo()).isNull();
            }
        }

        @Test
        @DisplayName("Basic separa por el primer : y decodifica cada parte como form-urlencoded")
        void basicPrimerDosPuntos() {
            PeticionToken peticion = interpretar(FORM, List.of(basic("voting%5Fapi:se:cre%3At")),
                    "grant_type=client_credentials");

            assertThat(peticion.clientIdCrudo()).isEqualTo("voting_api");
            assertThat(peticion.secreto().valor()).isEqualTo("se:cre:t");
        }

        @Test
        @DisplayName("Debe ignorar charset en el Content-Type y los parámetros desconocidos, aunque estén repetidos")
        void ignoraCharsetYDesconocidos() {
            PeticionToken peticion = interpretar("Application/X-WWW-Form-Urlencoded; charset=UTF-8", List.of(),
                    "audience=x&audience=y&grant_type=client_credentials&client_id=a&client_secret=b");

            assertThat(peticion.clientIdCrudo()).isEqualTo("a");
        }

        @Test
        @DisplayName("Debe decodificar el scope conservando : y /")
        void decodificaScope() {
            PeticionToken peticion = interpretar(FORM, List.of(),
                    "grant_type=client_credentials&client_id=a&client_secret=b&scope=rabbitmq.read%3Avotos%2Fcola");

            assertThat(peticion.scopeCrudo()).isEqualTo("rabbitmq.read:votos/cola");
        }

        @Test
        @DisplayName("Debe pasar el client_id y el scope crudos, sin validar su formato")
        void pasaCrudos() {
            PeticionToken peticion = interpretar(FORM, List.of(),
                    "grant_type=client_credentials&client_id=a+b&client_secret=b&scope=a%22b");

            assertThat(peticion.clientIdCrudo()).isEqualTo("a b");
            assertThat(peticion.scopeCrudo()).isEqualTo("a\"b");
        }

        @Test
        @DisplayName("Un parámetro sin valor se trata como no enviado")
        void parametroVacioComoAusente() {
            PeticionToken peticion = interpretar(FORM, List.of(),
                    "grant_type=client_credentials&client_id=a&client_secret=b&scope=");

            assertThat(peticion.scopeCrudo()).isNull();
        }
    }

    @Nested
    @DisplayName("invalid_request")
    class Invalidas {

        private static final String VALIDO = "grant_type=client_credentials&client_id=a&client_secret=b";

        @ParameterizedTest
        @ValueSource(strings = {"application/json", "text/plain", "multipart/form-data"})
        @DisplayName("Debe rechazar un Content-Type distinto de form-urlencoded")
        void rechazaContentType(String contentType) {
            rechaza(contentType, List.of(), cuerpo(VALIDO));
        }

        @Test
        @DisplayName("Debe rechazar la ausencia de Content-Type y el cuerpo vacío")
        void rechazaSinContentTypeOVacio() {
            rechaza(null, List.of(), cuerpo(VALIDO));
            rechaza(FORM, List.of(), new byte[0]);
        }

        @ParameterizedTest
        @ValueSource(strings = {"grant_type=client_credentials&client_id=%ZZ&client_secret=b",
                "grant_type=client_credentials&client_id=%4&client_secret=b",
                "grant_type&client_id=a&client_secret=b",
                "grant_type=client_credentials&&client_id=a&client_secret=b"})
        @DisplayName("Debe rechazar percent-encoding inválido o pares sin =")
        void rechazaCodificacion(String texto) {
            rechaza(FORM, List.of(), cuerpo(texto));
        }

        @Test
        @DisplayName("Debe rechazar bytes UTF-8 inválidos, en crudo o con percent-encoding")
        void rechazaUtf8Invalido() {
            rechaza(FORM, List.of(), cuerpo("grant_type=client_credentials&client_id=%C3%28&client_secret=b"));
            byte[] crudo = cuerpo("grant_type=client_credentials&client_secret=b&client_id=X");
            crudo[crudo.length - 1] = (byte) 0xFF;
            rechaza(FORM, List.of(), crudo);
        }

        @ParameterizedTest
        @ValueSource(strings = {"grant_type", "scope", "client_id", "client_secret"})
        @DisplayName("Debe rechazar un parámetro conocido repetido")
        void rechazaRepetidos(String parametro) {
            rechaza(FORM, List.of(), cuerpo(VALIDO + "&scope=x&" + parametro + "=y"));
        }

        @Test
        @DisplayName("Debe rechazar la falta de grant_type")
        void rechazaSinGrant() {
            rechaza(FORM, List.of(), cuerpo("client_id=a&client_secret=b"));
            rechaza(FORM, List.of(), cuerpo("grant_type=&client_id=a&client_secret=b"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"a%09b", "a%C2%A0b", "%0Aa"})
        @DisplayName("Debe rechazar espacios en blanco distintos de U+0020 en scope")
        void rechazaEspaciosEnScope(String scope) {
            rechaza(FORM, List.of(), cuerpo(VALIDO + "&scope=" + scope));
        }

        @Test
        @DisplayName("Debe rechazar credenciales ausentes o vacías por cualquiera de las dos vías")
        void rechazaCredencialesVacias() {
            rechaza(FORM, List.of(), cuerpo("grant_type=client_credentials&client_id=a"));
            rechaza(FORM, List.of(), cuerpo("grant_type=client_credentials&client_secret=b"));
            rechaza(FORM, List.of(), cuerpo("grant_type=client_credentials&client_id=&client_secret=b"));
            rechaza(FORM, List.of(basic("a:")), cuerpo("grant_type=client_credentials"));
            rechaza(FORM, List.of(basic(":b")), cuerpo("grant_type=client_credentials"));
        }

        @Test
        @DisplayName("Debe rechazar un valor Basic inválido")
        void rechazaBasicInvalido() {
            rechaza(FORM, List.of("Basic @@@"), cuerpo("grant_type=client_credentials"));
            rechaza(FORM, List.of(basic("sin-dos-puntos")), cuerpo("grant_type=client_credentials"));
            rechaza(FORM, List.of(basic("a%ZZ:b")), cuerpo("grant_type=client_credentials"));
            rechaza(FORM, List.of("Basic"), cuerpo("grant_type=client_credentials"));
        }

        @Test
        @DisplayName("Debe rechazar Basic junto con credenciales en el cuerpo, aunque coincidan")
        void rechazaBasicMasCuerpo() {
            PeticionMalFormadaException error = rechaza(FORM, List.of(basic("a:b")),
                    cuerpo("grant_type=client_credentials&client_id=a"));
            rechaza(FORM, List.of(basic("a:b")), cuerpo("grant_type=client_credentials&client_secret=b"));

            assertThat(error.clientIdCrudo()).contains("a");
        }

        @Test
        @DisplayName("Debe rechazar un esquema distinto de Basic o varias cabeceras Authorization")
        void rechazaOtroEsquemaOVarias() {
            rechaza(FORM, List.of("Bearer abc"), cuerpo("grant_type=client_credentials"));
            rechaza(FORM, List.of("Bearer abc"), cuerpo(VALIDO));
            rechaza(FORM, List.of(basic("a:b"), basic("a:b")), cuerpo("grant_type=client_credentials"));
        }

        @Test
        @DisplayName("Si ya conoce el client_id del cuerpo, debe adjuntarlo a la excepción")
        void adjuntaClientId() {
            PeticionMalFormadaException error = rechaza(FORM, List.of(),
                    cuerpo("client_id=voting_api&client_secret=b"));

            assertThat(error.clientIdCrudo()).contains("voting_api");
        }
    }
}
