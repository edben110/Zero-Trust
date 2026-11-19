package com.zerotrust.analyticsarchiver.security;

import java.util.Map;

// Propiedades de kafka-clients 3.6 (gestionado por Spring Boot 3.2.5) para SASL/OAUTHBEARER contra auth_server.
public final class PropiedadesKafkaOAuth {

    static final String HANDLER = "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginCallbackHandler";
    static final String MODULO = "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule";

    private PropiedadesKafkaOAuth() {
    }

    public static Map<String, Object> propiedades(VariablesClienteToken variables) {
        return Map.of(
                "security.protocol", variables.kafkaSecurityProtocol(),
                "sasl.mechanism", "OAUTHBEARER",
                "sasl.login.callback.handler.class", HANDLER,
                "sasl.oauthbearer.token.endpoint.url", variables.authTokenUrl(),
                "sasl.login.refresh.buffer.seconds", "30",
                "sasl.jaas.config", jaas(variables),
                "ssl.truststore.location", variables.truststoreFile(),
                "ssl.truststore.password", variables.truststorePassword(),
                "ssl.truststore.type", "PKCS12");
    }

    // Las opciones ssl.* del JAAS son las del HTTPS hacia AUTH_TOKEN_URL; las de nivel superior, las del broker.
    static String jaas(VariablesClienteToken variables) {
        return MODULO + " required"
                + " clientId=" + citar(variables.clientId())
                + " clientSecret=" + citar(variables.clientSecret())
                + " ssl.truststore.location=" + citar(variables.truststoreFile())
                + " ssl.truststore.password=" + citar(variables.truststorePassword())
                + " ssl.truststore.type=\"PKCS12\";";
    }

    private static String citar(String valor) {
        return "\"" + valor.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
