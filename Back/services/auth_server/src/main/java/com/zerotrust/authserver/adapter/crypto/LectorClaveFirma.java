package com.zerotrust.authserver.adapter.crypto;

import com.zerotrust.authserver.domain.ConfiguracionInvalidaException;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

public class LectorClaveFirma {

    private static final String ORIGEN = "AUTH_SIGNING_KEY_FILE";
    private static final String INICIO = "-----BEGIN PRIVATE KEY-----";
    private static final String FIN = "-----END PRIVATE KEY-----";
    private static final int BITS_MINIMOS = 2048;

    public KeyPair leer(byte[] contenido) {
        String texto = new String(contenido, StandardCharsets.US_ASCII).strip();
        if (!texto.startsWith(INICIO) || !texto.endsWith(FIN)) {
            throw invalida("debe ser una clave privada en PEM PKCS#8 sin cifrar");
        }
        byte[] der = decodificar(texto.substring(INICIO.length(), texto.length() - FIN.length()));
        RSAPrivateCrtKey privada = privadaRsa(der);
        if (privada.getModulus().bitLength() < BITS_MINIMOS) {
            throw invalida("la clave RSA debe tener al menos 2048 bits");
        }
        return new KeyPair(publicaDe(privada.getModulus(), privada.getPublicExponent()), privada);
    }

    private static byte[] decodificar(String cuerpo) {
        try {
            return Base64.getMimeDecoder().decode(cuerpo);
        } catch (IllegalArgumentException e) {
            throw invalida("el contenido PEM no es Base64 válido");
        }
    }

    private static RSAPrivateCrtKey privadaRsa(byte[] der) {
        try {
            PrivateKey clave = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
            if (clave instanceof RSAPrivateCrtKey crt) {
                return crt;
            }
            throw invalida("la clave RSA debe incluir sus parámetros CRT");
        } catch (GeneralSecurityException e) {
            throw invalida("no contiene una clave privada RSA válida");
        }
    }

    private static RSAPublicKey publicaDe(BigInteger modulo, BigInteger exponente) {
        try {
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulo, exponente));
        } catch (GeneralSecurityException e) {
            throw invalida("no se pudo derivar la clave pública");
        }
    }

    private static ConfiguracionInvalidaException invalida(String detalle) {
        return new ConfiguracionInvalidaException(ORIGEN + ": " + detalle);
    }
}
