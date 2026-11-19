package com.zerotrust.authserver.adapter.crypto;

import com.zerotrust.authserver.exception.ConfiguracionInvalidaException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

// Se valida antes de levantar Spring, porque Tomcat carga el keystore al crear el servidor (plan D2).
public class ValidadorKeystore {

    private static final String ORIGEN = "AUTH_TLS_KEYSTORE";

    private final Clock reloj;

    public ValidadorKeystore(Clock reloj) {
        this.reloj = reloj;
    }

    public void validar(byte[] contenido, char[] contrasena) {
        KeyStore almacen = cargar(contenido, contrasena);
        List<X509Certificate> certificados = certificadosConClavePrivada(almacen, contrasena);
        if (certificados.isEmpty()) {
            throw invalida("no contiene una clave privada con su certificado");
        }
        if (certificados.size() > 1) {
            throw invalida("contiene más de una entrada con clave privada");
        }
        comprobarVigencia(certificados.get(0));
    }

    private static KeyStore cargar(byte[] contenido, char[] contrasena) {
        try {
            KeyStore almacen = KeyStore.getInstance("PKCS12");
            almacen.load(new ByteArrayInputStream(contenido), contrasena);
            return almacen;
        } catch (IOException | GeneralSecurityException e) {
            throw invalida("no se puede abrir el keystore PKCS12 o la contraseña es incorrecta");
        }
    }

    private static List<X509Certificate> certificadosConClavePrivada(KeyStore almacen, char[] contrasena) {
        try {
            List<X509Certificate> resultado = new ArrayList<>();
            for (String alias : Collections.list(almacen.aliases())) {
                if (almacen.isKeyEntry(alias)
                        && almacen.getKey(alias, contrasena) instanceof PrivateKey
                        && almacen.getCertificate(alias) instanceof X509Certificate certificado) {
                    resultado.add(certificado);
                }
            }
            return resultado;
        } catch (GeneralSecurityException e) {
            throw invalida("no se pueden leer las entradas del keystore");
        }
    }

    private void comprobarVigencia(X509Certificate certificado) {
        try {
            certificado.checkValidity(Date.from(reloj.instant()));
        } catch (CertificateExpiredException e) {
            throw invalida("el certificado TLS está caducado");
        } catch (CertificateNotYetValidException e) {
            throw invalida("el certificado TLS todavía no es válido");
        }
    }

    private static ConfiguracionInvalidaException invalida(String detalle) {
        return new ConfiguracionInvalidaException(ORIGEN + ": " + detalle);
    }
}
