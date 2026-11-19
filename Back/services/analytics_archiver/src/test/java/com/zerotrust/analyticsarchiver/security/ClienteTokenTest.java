package com.zerotrust.analyticsarchiver.security;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClienteTokenTest {

    private static Map<String, String> entornoValido() {
        Map<String, String> entorno = new HashMap<>();
        entorno.put("AUTH_TOKEN_URL", "https://vm-auth/token");
        entorno.put("CLIENT_ID", "servicio");
        entorno.put("CLIENT_SECRET", "s3cr3t");
        entorno.put("TRUSTSTORE_FILE", "/certs/ca.p12");
        entorno.put("TRUSTSTORE_PASSWORD", "clave");
        return entorno;
    }

    @Nested
    @DisplayName("VariablesClienteToken")
    class Variables {

        @Test
        @DisplayName("Debe leer las variables y usar SASL_SSL por defecto")
        void leeVariables() {
            VariablesClienteToken variables = VariablesClienteToken.desde(entornoValido()::get);

            assertThat(variables.clientId()).isEqualTo("servicio");
            assertThat(variables.kafkaSecurityProtocol()).isEqualTo("SASL_SSL");
            assertThat(variables.toString()).doesNotContain("s3cr3t").doesNotContain("clave");
        }

        @ParameterizedTest
        @ValueSource(strings = {"AUTH_TOKEN_URL", "CLIENT_ID", "CLIENT_SECRET", "TRUSTSTORE_FILE", "TRUSTSTORE_PASSWORD"})
        @DisplayName("Debe impedir el arranque si falta o está vacía una variable obligatoria")
        void faltaObligatoria(String nombre) {
            Map<String, String> entorno = entornoValido();
            entorno.remove(nombre);
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> VariablesClienteToken.desde(entorno::get));
            assertThat(error.getMessage()).contains(nombre);

            entorno.put(nombre, " ");
            assertThrows(IllegalStateException.class, () -> VariablesClienteToken.desde(entorno::get));
        }

        @Test
        @DisplayName("Debe aceptar SASL_PLAINTEXT y rechazar otros protocolos")
        void protocolo() {
            Map<String, String> entorno = entornoValido();
            entorno.put("KAFKA_SECURITY_PROTOCOL", "SASL_PLAINTEXT");
            assertThat(VariablesClienteToken.desde(entorno::get).kafkaSecurityProtocol()).isEqualTo("SASL_PLAINTEXT");

            entorno.put("KAFKA_SECURITY_PROTOCOL", "PLAINTEXT");
            assertThrows(IllegalStateException.class, () -> VariablesClienteToken.desde(entorno::get));
        }
    }

    @Nested
    @DisplayName("PropiedadesKafkaOAuth")
    class Kafka {

        @Test
        @DisplayName("Debe configurar OAUTHBEARER contra AUTH_TOKEN_URL con el handler vigente y buffer de 30 s")
        void propiedades() {
            Map<String, Object> propiedades = PropiedadesKafkaOAuth.propiedades(VariablesClienteToken.desde(entornoValido()::get));

            assertThat(propiedades)
                    .containsEntry("security.protocol", "SASL_SSL")
                    .containsEntry("sasl.mechanism", "OAUTHBEARER")
                    .containsEntry("sasl.login.callback.handler.class",
                            "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginCallbackHandler")
                    .containsEntry("sasl.oauthbearer.token.endpoint.url", "https://vm-auth/token")
                    .containsEntry("sasl.login.refresh.buffer.seconds", "30")
                    .containsEntry("ssl.truststore.location", "/certs/ca.p12")
                    .containsEntry("ssl.truststore.type", "PKCS12");
        }

        @Test
        @DisplayName("El JAAS debe llevar las credenciales y el truststore del endpoint de token")
        void jaas() {
            String jaas = PropiedadesKafkaOAuth.jaas(VariablesClienteToken.desde(entornoValido()::get));

            assertThat(jaas).startsWith("org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required ")
                    .contains("clientId=\"servicio\"")
                    .contains("clientSecret=\"s3cr3t\"")
                    .contains("ssl.truststore.location=\"/certs/ca.p12\"")
                    .endsWith(";");
        }

        @Test
        @DisplayName("El JAAS debe escapar comillas y barras invertidas")
        void escapa() {
            Map<String, String> entorno = entornoValido();
            entorno.put("CLIENT_SECRET", "a\"b\\c");
            entorno.put("TRUSTSTORE_FILE", "C:\\certs\\ca.p12");

            String jaas = PropiedadesKafkaOAuth.jaas(VariablesClienteToken.desde(entorno::get));

            assertThat(jaas).contains("clientSecret=\"a\\\"b\\\\c\"")
                    .contains("ssl.truststore.location=\"C:\\\\certs\\\\ca.p12\"");
        }
    }

    @Nested
    @DisplayName("ContextoTls")
    class Tls {

        private byte[] truststore(boolean conCertificado, char[] clave) throws Exception {
            KeyStore almacen = KeyStore.getInstance("PKCS12");
            almacen.load(null, null);
            if (conCertificado) {
                KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
                generador.initialize(2048);
                KeyPair par = generador.generateKeyPair();
                X500Name nombre = new X500Name("CN=CA del proyecto");
                var certificado = new JcaX509CertificateConverter().getCertificate(new JcaX509v3CertificateBuilder(
                        nombre, BigInteger.ONE, Date.from(Instant.now().minus(Duration.ofDays(1))),
                        Date.from(Instant.now().plus(Duration.ofDays(30))), nombre, par.getPublic())
                        .build(new JcaContentSignerBuilder("SHA256withRSA").build(par.getPrivate())));
                almacen.setCertificateEntry("ca", certificado);
            }
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            almacen.store(salida, clave);
            return salida.toByteArray();
        }

        @Test
        @DisplayName("Debe crear un SSLContext con el truststore de la CA")
        void creaContexto() throws Exception {
            assertThat(ContextoTls.desde(truststore(true, "clave".toCharArray()), "clave".toCharArray()).getProtocol())
                    .isEqualTo("TLS");
        }

        @Test
        @DisplayName("Debe impedir el arranque con un truststore vacío, corrupto, con otra contraseña o ilegible")
        void rechazaInvalidos() throws Exception {
            byte[] valido = truststore(true, "clave".toCharArray());
            assertThrows(IllegalStateException.class, () -> ContextoTls.desde(truststore(false, "clave".toCharArray()), "clave".toCharArray()));
            assertThrows(IllegalStateException.class, () -> ContextoTls.desde(new byte[] {1, 2, 3}, "clave".toCharArray()));
            assertThrows(IllegalStateException.class, () -> ContextoTls.desde(valido, "otra".toCharArray()));
            assertThrows(IllegalStateException.class, () -> ContextoTls.desdeArchivo("/no/existe/ca.p12", "clave"));
        }
    }
}
