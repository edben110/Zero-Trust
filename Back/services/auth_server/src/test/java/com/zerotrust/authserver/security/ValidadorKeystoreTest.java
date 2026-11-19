package com.zerotrust.authserver.security;

import com.zerotrust.authserver.exception.ConfiguracionInvalidaException;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidadorKeystoreTest {

    private static final char[] CLAVE = "clave-keystore".toCharArray();
    private static final Instant AHORA = Instant.parse("2026-11-14T12:00:00Z");
    private static KeyPair par;

    private final ValidadorKeystore validador = new ValidadorKeystore(Clock.fixed(AHORA, ZoneOffset.UTC));

    @BeforeAll
    static void generarClaves() throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        par = generador.generateKeyPair();
    }

    private static X509Certificate certificado(Instant desde, Instant hasta) throws Exception {
        X500Name nombre = new X500Name("CN=vm-auth");
        var constructor = new JcaX509v3CertificateBuilder(nombre, BigInteger.ONE, Date.from(desde), Date.from(hasta),
                nombre, par.getPublic());
        var firmante = new JcaContentSignerBuilder("SHA256withRSA").build(par.getPrivate());
        return new JcaX509CertificateConverter().getCertificate(constructor.build(firmante));
    }

    private static byte[] keystore(int entradasConClave, Instant desde, Instant hasta) throws Exception {
        KeyStore almacen = KeyStore.getInstance("PKCS12");
        almacen.load(null, null);
        X509Certificate certificado = certificado(desde, hasta);
        for (int i = 0; i < entradasConClave; i++) {
            almacen.setKeyEntry("tls-" + i, par.getPrivate(), CLAVE, new Certificate[] {certificado});
        }
        if (entradasConClave == 0) {
            almacen.setCertificateEntry("ca", certificado);
        }
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        almacen.store(salida, CLAVE);
        return salida.toByteArray();
    }

    private static byte[] vigente(int entradas) throws Exception {
        return keystore(entradas, AHORA.minus(Duration.ofDays(1)), AHORA.plus(Duration.ofDays(365)));
    }

    @Test
    @DisplayName("Debe aceptar un keystore con una clave privada y un certificado vigente")
    void aceptaValido() throws Exception {
        byte[] contenido = vigente(1);

        assertThatCode(() -> validador.validar(contenido, CLAVE)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Debe rechazar un keystore corrupto")
    void rechazaCorrupto() {
        byte[] basura = new byte[512];
        new Random(7).nextBytes(basura);

        assertThrows(ConfiguracionInvalidaException.class, () -> validador.validar(basura, CLAVE));
    }

    @Test
    @DisplayName("Debe rechazar una contraseña incorrecta")
    void rechazaContrasenaIncorrecta() throws Exception {
        byte[] contenido = vigente(1);

        assertThrows(ConfiguracionInvalidaException.class, () -> validador.validar(contenido, "otra".toCharArray()));
    }

    @Test
    @DisplayName("Debe rechazar un keystore sin clave privada")
    void rechazaSinClave() throws Exception {
        byte[] contenido = vigente(0);

        assertThrows(ConfiguracionInvalidaException.class, () -> validador.validar(contenido, CLAVE));
    }

    @Test
    @DisplayName("Debe rechazar un keystore con dos entradas de clave privada")
    void rechazaDosClaves() throws Exception {
        byte[] contenido = vigente(2);

        ConfiguracionInvalidaException error = assertThrows(ConfiguracionInvalidaException.class,
                () -> validador.validar(contenido, CLAVE));
        assertThat(error.getMessage()).contains("más de una");
    }

    @Test
    @DisplayName("Debe rechazar un certificado caducado")
    void rechazaCaducado() throws Exception {
        byte[] contenido = keystore(1, AHORA.minus(Duration.ofDays(400)), AHORA.minus(Duration.ofDays(1)));

        assertThrows(ConfiguracionInvalidaException.class, () -> validador.validar(contenido, CLAVE));
    }

    @Test
    @DisplayName("Debe rechazar un certificado con notBefore en el futuro")
    void rechazaNoVigenteAun() throws Exception {
        byte[] contenido = keystore(1, AHORA.plus(Duration.ofDays(1)), AHORA.plus(Duration.ofDays(400)));

        assertThrows(ConfiguracionInvalidaException.class, () -> validador.validar(contenido, CLAVE));
    }
}
