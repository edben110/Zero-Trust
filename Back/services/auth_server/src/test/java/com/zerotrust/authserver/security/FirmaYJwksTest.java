package com.zerotrust.authserver.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.zerotrust.authserver.domain.Audiencia;
import com.zerotrust.authserver.domain.ClaimsToken;
import com.zerotrust.authserver.exception.ConfiguracionInvalidaException;
import com.zerotrust.authserver.domain.IdClave;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FirmaYJwksTest {

    private static KeyPair claves2048;
    private static final IdClave KID = new IdClave("clave-1");
    private static final ObjectMapper JSON = new ObjectMapper();

    @BeforeAll
    static void generarClaves() throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        claves2048 = generador.generateKeyPair();
    }

    static byte[] pem(String tipo, byte[] der) {
        String cuerpo = Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(der);
        return ("-----BEGIN " + tipo + "-----\n" + cuerpo + "\n-----END " + tipo + "-----\n")
                .getBytes(StandardCharsets.US_ASCII);
    }

    @Nested
    @DisplayName("LectorClaveFirma")
    class LectorClaveFirmaTest {

        private final LectorClaveFirma lector = new LectorClaveFirma();

        @Test
        @DisplayName("Debe leer una clave RSA de 2048 bits en PEM PKCS#8 y derivar la pública")
        void leeClaveValida() {
            // Arrange
            byte[] contenido = pem("PRIVATE KEY", claves2048.getPrivate().getEncoded());

            // Act
            KeyPair leido = lector.leer(contenido);

            // Assert
            assertThat(leido.getPrivate()).isEqualTo(claves2048.getPrivate());
            assertThat(((RSAPublicKey) leido.getPublic()).getModulus())
                    .isEqualTo(((RSAPublicKey) claves2048.getPublic()).getModulus());
        }

        @Test
        @DisplayName("Debe rechazar PKCS#1 y PEM cifrado")
        void rechazaOtrosFormatos() {
            byte[] der = claves2048.getPrivate().getEncoded();
            assertThrows(ConfiguracionInvalidaException.class, () -> lector.leer(pem("RSA PRIVATE KEY", der)));
            assertThrows(ConfiguracionInvalidaException.class, () -> lector.leer(pem("ENCRYPTED PRIVATE KEY", der)));
        }

        @Test
        @DisplayName("Debe rechazar claves de menos de 2048 bits")
        void rechazaClaveCorta() throws Exception {
            KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
            generador.initialize(1024);
            byte[] contenido = pem("PRIVATE KEY", generador.generateKeyPair().getPrivate().getEncoded());

            assertThrows(ConfiguracionInvalidaException.class, () -> lector.leer(contenido));
        }

        @Test
        @DisplayName("Debe rechazar claves que no son RSA y contenido que no es PEM")
        void rechazaNoRsa() throws Exception {
            KeyPairGenerator generador = KeyPairGenerator.getInstance("EC");
            generador.initialize(256);
            byte[] ec = pem("PRIVATE KEY", generador.generateKeyPair().getPrivate().getEncoded());

            assertThrows(ConfiguracionInvalidaException.class, () -> lector.leer(ec));
            assertThrows(ConfiguracionInvalidaException.class, () -> lector.leer("no es pem".getBytes(StandardCharsets.UTF_8)));
            assertThrows(ConfiguracionInvalidaException.class,
                    () -> lector.leer("-----BEGIN PRIVATE KEY-----\n@@@\n-----END PRIVATE KEY-----".getBytes(StandardCharsets.UTF_8)));
        }
    }

    @Nested
    @DisplayName("FirmadorJws y PublicadorJwksNimbus")
    class FirmaTest {

        private final FirmadorJws firmador = new FirmadorJws((java.security.interfaces.RSAPrivateKey) claves2048.getPrivate(), KID);
        private final PublicadorJwksNimbus publicador = new PublicadorJwksNimbus((RSAPublicKey) claves2048.getPublic(), KID);

        private ClaimsToken claims(List<Audiencia> aud, Optional<String> scope) {
            return new ClaimsToken("https://vm-auth", "voting_api", aud, scope, 1_000L, 1_300L);
        }

        @Test
        @DisplayName("El token debe verificarse con la clave del JWK Set publicado y llevar kid y RS256")
        void verificaConJwks() throws Exception {
            // Arrange
            String token = firmador.firmar(claims(List.of(Audiencia.RABBITMQ, Audiencia.KAFKA), Optional.of("a b")));
            RSAKey clavePublicada = (RSAKey) JWKSet.parse(publicador.jwkSetPublico()).getKeyByKeyId("clave-1");

            // Act
            JWSObject jws = JWSObject.parse(token);

            // Assert
            assertThat(jws.verify(new RSASSAVerifier(clavePublicada))).isTrue();
            assertThat(jws.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.RS256);
            assertThat(jws.getHeader().getKeyID()).isEqualTo("clave-1");
        }

        @Test
        @DisplayName("El payload debe llevar exactamente los claims de RF-4, con aud como array en el orden del archivo")
        void payloadExacto() throws Exception {
            String token = firmador.firmar(claims(List.of(Audiencia.RABBITMQ, Audiencia.KAFKA), Optional.of("a b")));

            JsonNode payload = JSON.readTree(JWSObject.parse(token).getPayload().toString());

            assertThat(payload.properties()).extracting(Map.Entry::getKey)
                    .containsExactlyInAnyOrder("iss", "sub", "aud", "scope", "iat", "exp");
            assertThat(payload.get("aud").isArray()).isTrue();
            assertThat(payload.get("aud").get(0).asText()).isEqualTo("rabbitmq");
            assertThat(payload.get("aud").get(1).asText()).isEqualTo("kafka");
            assertThat(payload.get("scope").asText()).isEqualTo("a b");
            assertThat(payload.get("iat").asLong()).isEqualTo(1_000L);
            assertThat(payload.get("exp").asLong()).isEqualTo(1_300L);
        }

        @Test
        @DisplayName("Con una sola audiencia aud sigue siendo array, y sin scopes no hay claim scope")
        void unaAudienciaYSinScope() throws Exception {
            String token = firmador.firmar(claims(List.of(Audiencia.KAFKA), Optional.empty()));

            JsonNode payload = JSON.readTree(JWSObject.parse(token).getPayload().toString());

            assertThat(payload.get("aud").isArray()).isTrue();
            assertThat(payload.get("aud")).hasSize(1);
            assertThat(payload.has("scope")).isFalse();
        }

        @Test
        @DisplayName("Documenta que JWTClaimsSet de nimbus colapsa una audiencia única a string (motivo de D5)")
        void nimbusColapsaAud() {
            Object aud = new JWTClaimsSet.Builder().audience(List.of("kafka")).build().toJSONObject().get("aud");

            assertThat(aud).isInstanceOf(String.class);
        }

        @Test
        @DisplayName("El JWK Set real debe llevar kty, alg, use y kid, sin material privado, y ser inmutable")
        void jwkSetReal() {
            Map<String, Object> jwkSet = publicador.jwkSetPublico();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> claves = (List<Map<String, Object>>) jwkSet.get("keys");
            assertThat(claves).hasSize(1);
            Map<String, Object> clave = claves.get(0);
            assertThat(clave).containsEntry("kty", "RSA").containsEntry("alg", "RS256")
                    .containsEntry("use", "sig").containsEntry("kid", "clave-1");
            assertThat(clave).doesNotContainKeys("d", "p", "q", "dp", "dq", "qi");
            assertThat((String) clave.get("n")).doesNotContain("=").doesNotContain("+").doesNotContain("/");
            assertThat((String) clave.get("e")).isEqualTo("AQAB");
            assertThrows(UnsupportedOperationException.class, () -> jwkSet.put("x", "y"));
            assertThrows(UnsupportedOperationException.class, () -> clave.put("x", "y"));
        }
    }
}
