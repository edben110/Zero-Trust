package com.zerotrust.analyticsarchiver.config;

import java.util.Set;
import java.util.function.Function;

public record VariablesClienteToken(String authTokenUrl, String clientId, String clientSecret, String truststoreFile,
                                    String truststorePassword, String kafkaSecurityProtocol) {

    private static final String PROTOCOLO_POR_DEFECTO = "SASL_SSL";
    private static final Set<String> PROTOCOLOS = Set.of("SASL_SSL", "SASL_PLAINTEXT");

    public VariablesClienteToken {
        if (!PROTOCOLOS.contains(kafkaSecurityProtocol)) {
            throw new IllegalStateException("KAFKA_SECURITY_PROTOCOL debe ser SASL_SSL o SASL_PLAINTEXT");
        }
    }

    // Si falta una variable obligatoria, el contexto de Spring no arranca.
    public static VariablesClienteToken desde(Function<String, String> entorno) {
        String protocolo = entorno.apply("KAFKA_SECURITY_PROTOCOL");
        return new VariablesClienteToken(
                obligatoria(entorno, "AUTH_TOKEN_URL"),
                obligatoria(entorno, "CLIENT_ID"),
                obligatoria(entorno, "CLIENT_SECRET"),
                obligatoria(entorno, "TRUSTSTORE_FILE"),
                obligatoria(entorno, "TRUSTSTORE_PASSWORD"),
                protocolo == null || protocolo.isBlank() ? PROTOCOLO_POR_DEFECTO : protocolo);
    }

    private static String obligatoria(Function<String, String> entorno, String nombre) {
        String valor = entorno.apply(nombre);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(nombre + ": variable obligatoria sin definir");
        }
        return valor;
    }

    @Override
    public String toString() {
        return "VariablesClienteToken[authTokenUrl=" + authTokenUrl + ", clientId=" + clientId + "]";
    }
}
