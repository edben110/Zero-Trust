package com.zerotrust.botdetector.security;

import org.springframework.boot.autoconfigure.kafka.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Map;

@Configuration
public class SeguridadKafkaOAuth {

    private final Environment entorno;

    public SeguridadKafkaOAuth(Environment entorno) {
        this.entorno = entorno;
    }

    @Bean
    DefaultKafkaConsumerFactoryCustomizer consumidorKafkaOAuth() {
        Map<String, Object> config = config();
        return factory -> factory.updateConfigs(config);
    }

    @Bean
    DefaultKafkaProducerFactoryCustomizer productorKafkaOAuth() {
        Map<String, Object> config = config();
        return factory -> factory.updateConfigs(config);
    }

    private Map<String, Object> config() {
        String truststore = requerida("TRUSTSTORE_FILE");
        String password = requerida("TRUSTSTORE_PASSWORD");
        String jaas = "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required"
                + " clientId=" + citar(requerida("CLIENT_ID"))
                + " clientSecret=" + citar(requerida("CLIENT_SECRET"))
                + " ssl.truststore.location=" + citar(truststore)
                + " ssl.truststore.password=" + citar(password)
                + " ssl.truststore.type=\"PKCS12\";";
        return Map.of(
                "security.protocol", "SASL_SSL",
                "sasl.mechanism", "OAUTHBEARER",
                "sasl.login.callback.handler.class",
                    "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginCallbackHandler",
                "sasl.oauthbearer.token.endpoint.url", requerida("AUTH_TOKEN_URL"),
                "sasl.jaas.config", jaas,
                "ssl.truststore.location", truststore,
                "ssl.truststore.password", password,
                "ssl.truststore.type", "PKCS12");
    }

    private String requerida(String key) {
        String value = entorno.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(key + ": variable obligatoria sin definir");
        }
        return value;
    }

    private static String citar(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
