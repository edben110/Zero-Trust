package com.zerotrust.uservalidation.config;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

// SSLContext que solo confía en la CA del proyecto (truststore PKCS12).
public final class ContextoTls {

    private ContextoTls() {
    }

    public static SSLContext desdeArchivo(String ruta, String contrasena) {
        try {
            return desde(Files.readAllBytes(Path.of(ruta)), contrasena.toCharArray());
        } catch (IOException e) {
            throw new IllegalStateException("TRUSTSTORE_FILE: no se puede leer el archivo indicado");
        }
    }

    public static SSLContext desde(byte[] truststore, char[] contrasena) {
        try {
            KeyStore almacen = KeyStore.getInstance("PKCS12");
            almacen.load(new ByteArrayInputStream(truststore), contrasena);
            if (almacen.size() == 0) {
                throw new IllegalStateException("TRUSTSTORE_FILE: el truststore está vacío");
            }
            TrustManagerFactory confianza = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            confianza.init(almacen);
            SSLContext contexto = SSLContext.getInstance("TLS");
            contexto.init(null, confianza.getTrustManagers(), null);
            return contexto;
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("TRUSTSTORE_FILE: no se puede abrir el truststore o la contraseña es incorrecta");
        }
    }
}
