package com.zerotrust.authserver.config;

import com.zerotrust.authserver.security.LectorClaveFirma;
import com.zerotrust.authserver.security.ValidadorKeystore;
import com.zerotrust.authserver.exception.ConfiguracionInvalidaException;
import com.zerotrust.authserver.domain.Emisor;
import com.zerotrust.authserver.domain.IdClave;
import com.zerotrust.authserver.domain.PuertoServidor;
import com.zerotrust.authserver.domain.RegistroClientes;
import com.zerotrust.authserver.domain.Ttl;
import com.zerotrust.authserver.port.FuenteVariables;
import com.zerotrust.authserver.port.LectorArchivos;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.util.Optional;
import java.util.function.Function;

// Está en config/ y no en service/ porque compone clases concretas de varias capas (constitución §4).
public class CargadorConfiguracion {

    private final FuenteVariables variables;
    private final LectorArchivos lector;
    private final Clock reloj;

    public CargadorConfiguracion(FuenteVariables variables, LectorArchivos lector, Clock reloj) {
        this.variables = variables;
        this.lector = lector;
        this.reloj = reloj;
    }

    public ConfiguracionAuthServer cargar() {
        Emisor emisor = convertir("AUTH_ISSUER", obligatoria("AUTH_ISSUER"), Emisor::new);
        RegistroClientes registro = new InterpreteArchivoClientes().interpretar(archivo("AUTH_CLIENTS_FILE"));
        KeyPair claves = new LectorClaveFirma().leer(archivo("AUTH_SIGNING_KEY_FILE"));
        IdClave idClave = convertir("AUTH_SIGNING_KEY_ID", obligatoria("AUTH_SIGNING_KEY_ID"), IdClave::new);
        Ttl ttl = opcional("AUTH_TOKEN_TTL").map(texto -> convertir("AUTH_TOKEN_TTL", texto, Ttl::desde))
                .orElseGet(Ttl::porDefecto);
        PuertoServidor puerto = opcional("SERVER_PORT").map(texto -> convertir("SERVER_PORT", texto, PuertoServidor::desde))
                .orElseGet(PuertoServidor::porDefecto);
        byte[] keystore = archivo("AUTH_TLS_KEYSTORE");
        new ValidadorKeystore(reloj).validar(keystore, obligatoria("AUTH_TLS_KEYSTORE_PASSWORD").toCharArray());
        return new ConfiguracionAuthServer(registro, emisor, ttl, idClave, (RSAPrivateKey) claves.getPrivate(),
                (RSAPublicKey) claves.getPublic(), puerto);
    }

    private String obligatoria(String nombre) {
        return variables.valor(nombre)
                .filter(valor -> !valor.isEmpty())
                .orElseThrow(() -> new ConfiguracionInvalidaException(nombre + ": variable obligatoria sin definir"));
    }

    private Optional<String> opcional(String nombre) {
        return variables.valor(nombre);
    }

    private byte[] archivo(String variable) {
        String ruta = obligatoria(variable);
        try {
            return lector.leer(ruta);
        } catch (ConfiguracionInvalidaException e) {
            throw new ConfiguracionInvalidaException(variable + ": no se puede leer el archivo indicado");
        }
    }

    private static <T> T convertir(String variable, String texto, Function<String, T> conversor) {
        try {
            return conversor.apply(texto);
        } catch (IllegalArgumentException e) {
            throw new ConfiguracionInvalidaException(variable + ": " + e.getMessage());
        }
    }
}
