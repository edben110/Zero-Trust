package com.zerotrust.authserver.config;

import com.zerotrust.authserver.domain.ClientId;
import com.zerotrust.authserver.exception.ConfiguracionInvalidaException;
import com.zerotrust.authserver.domain.RegistroClientes;
import com.zerotrust.authserver.port.FuenteVariables;
import com.zerotrust.authserver.port.LectorArchivos;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class CargadorConfiguracionTest {

    private static final Instant AHORA = Instant.parse("2026-11-14T12:00:00Z");
    private static final String HASH = "$2b$12$" + "a".repeat(53);
    private static byte[] pemClave;
    private static byte[] keystore;

    @Mock
    private FuenteVariables variables;
    @Mock
    private LectorArchivos lector;

    private final Map<String, String> entorno = new HashMap<>();
    private final Map<String, byte[]> archivos = new HashMap<>();

    @BeforeAll
    static void generarMaterial() throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        KeyPair par = generador.generateKeyPair();
        pemClave = ("-----BEGIN PRIVATE KEY-----\n" + Base64.getMimeEncoder().encodeToString(par.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n").getBytes(StandardCharsets.US_ASCII);

        X500Name nombre = new X500Name("CN=vm-auth");
        var certificado = new JcaX509CertificateConverter().getCertificate(new JcaX509v3CertificateBuilder(nombre,
                BigInteger.ONE, Date.from(AHORA.minus(Duration.ofDays(1))), Date.from(AHORA.plus(Duration.ofDays(30))),
                nombre, par.getPublic()).build(new JcaContentSignerBuilder("SHA256withRSA").build(par.getPrivate())));
        KeyStore almacen = KeyStore.getInstance("PKCS12");
        almacen.load(null, null);
        almacen.setKeyEntry("tls", par.getPrivate(), "clave".toCharArray(), new Certificate[] {certificado});
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        almacen.store(salida, "clave".toCharArray());
        keystore = salida.toByteArray();
    }

    @BeforeEach
    void configuracionValida() {
        entorno.put("AUTH_ISSUER", "https://vm-auth");
        entorno.put("AUTH_CLIENTS_FILE", "/config/clientes.json");
        entorno.put("AUTH_SIGNING_KEY_FILE", "/config/firma.pem");
        entorno.put("AUTH_SIGNING_KEY_ID", "clave-1");
        entorno.put("AUTH_TLS_KEYSTORE", "/config/tls.p12");
        entorno.put("AUTH_TLS_KEYSTORE_PASSWORD", "clave");
        archivos.put("/config/clientes.json", ("{\"clients\":[{\"client_id\":\"voting_api\",\"secret_hash\":\"" + HASH
                + "\",\"scopes\":[\"a\"],\"audiences\":[\"kafka\",\"rabbitmq\"]}]}").getBytes(StandardCharsets.UTF_8));
        archivos.put("/config/firma.pem", pemClave);
        archivos.put("/config/tls.p12", keystore);
        lenient().when(variables.valor(anyString())).thenAnswer(i -> Optional.ofNullable(entorno.get(i.getArgument(0, String.class))));
        lenient().when(lector.leer(anyString())).thenAnswer(i -> {
            byte[] contenido = archivos.get(i.getArgument(0, String.class));
            if (contenido == null) {
                throw new ConfiguracionInvalidaException("no se puede leer el archivo");
            }
            return contenido;
        });
    }

    private ConfiguracionAuthServer cargar() {
        return new CargadorConfiguracion(variables, lector, Clock.fixed(AHORA, ZoneOffset.UTC)).cargar();
    }

    private ConfiguracionInvalidaException falla() {
        ConfiguracionInvalidaException error = assertThrows(ConfiguracionInvalidaException.class, this::cargar);
        assertThat(error.getCause()).isNull();
        assertThat(error.getMessage()).doesNotContain(HASH).doesNotContain("clave\"");
        return error;
    }

    @Test
    @DisplayName("Debe cargar una configuración válida con TTL 300 y puerto 8443 por defecto")
    void cargaValida() {
        ConfiguracionAuthServer configuracion = cargar();

        assertThat(configuracion.emisor().valor()).isEqualTo("https://vm-auth");
        assertThat(configuracion.idClave().valor()).isEqualTo("clave-1");
        assertThat(configuracion.ttl().segundos()).isEqualTo(300);
        assertThat(configuracion.puerto().valor()).isEqualTo(8443);
        assertThat(configuracion.registro().buscar(new ClientId("voting_api"))).isPresent();
        assertThat(configuracion.clavePublica().getModulus()).isEqualTo(
                ((java.security.interfaces.RSAPrivateCrtKey) configuracion.clavePrivada()).getModulus());
    }

    @Test
    @DisplayName("Debe usar AUTH_TOKEN_TTL y SERVER_PORT cuando están definidas")
    void usaVariablesOpcionales() {
        entorno.put("AUTH_TOKEN_TTL", "600");
        entorno.put("SERVER_PORT", "9443");

        ConfiguracionAuthServer configuracion = cargar();

        assertThat(configuracion.ttl().segundos()).isEqualTo(600);
        assertThat(configuracion.puerto().valor()).isEqualTo(9443);
    }

    @ParameterizedTest
    @ValueSource(strings = {"AUTH_ISSUER", "AUTH_CLIENTS_FILE", "AUTH_SIGNING_KEY_FILE", "AUTH_SIGNING_KEY_ID",
            "AUTH_TLS_KEYSTORE", "AUTH_TLS_KEYSTORE_PASSWORD"})
    @DisplayName("Debe no arrancar si falta o está vacía una variable obligatoria")
    void faltaObligatoria(String variable) {
        entorno.remove(variable);
        assertThat(falla().getMessage()).contains(variable);

        entorno.put(variable, "");
        assertThat(falla().getMessage()).contains(variable);
    }

    @Test
    @DisplayName("Debe no arrancar con un emisor que no es una URL https con host (RF-23)")
    void emisorInvalido() {
        entorno.put("AUTH_ISSUER", "http://vm-auth");
        assertThat(falla().getMessage()).contains("AUTH_ISSUER");
    }

    @Test
    @DisplayName("Debe no arrancar con un archivo de clientes inválido o ilegible (RF-16)")
    void clientesInvalidos() {
        archivos.put("/config/clientes.json", "{\"clients\":[]}".getBytes(StandardCharsets.UTF_8));
        assertThat(falla().getMessage()).contains("AUTH_CLIENTS_FILE");

        archivos.remove("/config/clientes.json");
        assertThat(falla().getMessage()).contains("AUTH_CLIENTS_FILE");
    }

    @Test
    @DisplayName("Debe no arrancar con una clave de firma inválida o ilegible (RF-18)")
    void claveInvalida() {
        archivos.put("/config/firma.pem", "no es pem".getBytes(StandardCharsets.UTF_8));
        assertThat(falla().getMessage()).contains("AUTH_SIGNING_KEY_FILE");

        archivos.remove("/config/firma.pem");
        assertThat(falla().getMessage()).contains("AUTH_SIGNING_KEY_FILE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "0", "59", "3601", "abc"})
    @DisplayName("Debe no arrancar con un AUTH_TOKEN_TTL inválido, incluido el vacío (RF-20)")
    void ttlInvalido(String valor) {
        entorno.put("AUTH_TOKEN_TTL", valor);
        assertThat(falla().getMessage()).contains("AUTH_TOKEN_TTL");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1023", "65536", "80a"})
    @DisplayName("Debe no arrancar con un SERVER_PORT inválido (RF-27)")
    void puertoInvalido(String valor) {
        entorno.put("SERVER_PORT", valor);
        assertThat(falla().getMessage()).contains("SERVER_PORT");
    }

    @Test
    @DisplayName("Debe no arrancar con un keystore inválido, ilegible o con otra contraseña (RF-22)")
    void keystoreInvalido() {
        entorno.put("AUTH_TLS_KEYSTORE_PASSWORD", "otra");
        assertThat(falla().getMessage()).contains("AUTH_TLS_KEYSTORE");

        entorno.put("AUTH_TLS_KEYSTORE_PASSWORD", "clave");
        archivos.remove("/config/tls.p12");
        assertThat(falla().getMessage()).contains("AUTH_TLS_KEYSTORE");
    }

    @Test
    @DisplayName("El archivo de clientes de ejemplo del repositorio debe cargarse sin errores")
    void ejemploCarga() throws Exception {
        byte[] ejemplo = Files.readAllBytes(Path.of("config", "clientes.ejemplo.json"));

        RegistroClientes registro = new InterpreteArchivoClientes().interpretar(ejemplo);

        assertThat(registro.buscar(new ClientId("voting_api"))).isPresent();
        assertThat(registro.buscar(new ClientId("analytics_archiver"))).isPresent();
        assertThat(registro.costeComun()).isEqualTo(12);
    }
}
