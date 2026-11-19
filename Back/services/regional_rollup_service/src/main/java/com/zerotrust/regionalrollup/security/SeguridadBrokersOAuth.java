package com.zerotrust.regionalrollup.config;

import com.rabbitmq.client.impl.CredentialsProvider;
import com.rabbitmq.client.impl.CredentialsRefreshService;
import com.rabbitmq.client.impl.DefaultCredentialsRefreshService.DefaultCredentialsRefreshServiceBuilder;
import com.rabbitmq.client.impl.OAuth2ClientCredentialsGrantCredentialsProvider.OAuth2ClientCredentialsGrantCredentialsProviderBuilder;
import org.springframework.boot.autoconfigure.amqp.ConnectionFactoryCustomizer;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Map;

/** JWT propio del servicio para consumir Kafka y publicar en RabbitMQ. */
@Configuration
public class SeguridadBrokersOAuth {

    private final Environment entorno;

    public SeguridadBrokersOAuth(Environment entorno) {
        this.entorno = entorno;
    }

    @Bean
    DefaultKafkaConsumerFactoryCustomizer consumidorKafkaOAuth() {
        String id = requerida("CLIENT_ID");
        String secret = requerida("CLIENT_SECRET");
        String truststore = requerida("TRUSTSTORE_FILE");
        String password = requerida("TRUSTSTORE_PASSWORD");
        String jaas = "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required"
                + " clientId=" + citar(id) + " clientSecret=" + citar(secret)
                + " ssl.truststore.location=" + citar(truststore)
                + " ssl.truststore.password=" + citar(password)
                + " ssl.truststore.type=\"PKCS12\";";
        Map<String, Object> config = Map.of(
                "security.protocol", "SASL_SSL",
                "sasl.mechanism", "OAUTHBEARER",
                "sasl.login.callback.handler.class",
                    "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginCallbackHandler",
                "sasl.oauthbearer.token.endpoint.url", requerida("AUTH_TOKEN_URL"),
                "sasl.jaas.config", jaas,
                "ssl.truststore.location", truststore,
                "ssl.truststore.password", password,
                "ssl.truststore.type", "PKCS12");
        return factory -> factory.updateConfigs(config);
    }

    @Bean
    ConnectionFactoryCustomizer rabbitOAuth() throws Exception {
        CredentialsProvider provider = new OAuth2ClientCredentialsGrantCredentialsProviderBuilder()
                .tokenEndpointUri(requerida("AUTH_TOKEN_URL"))
                .clientId(requerida("CLIENT_ID"))
                .clientSecret(requerida("CLIENT_SECRET"))
                .grantType("client_credentials")
                .tls().sslContext(contextoTls()).builder()
                .build();
        CredentialsRefreshService refresh = new DefaultCredentialsRefreshServiceBuilder().build();
        return factory -> {
            factory.setCredentialsProvider(provider);
            factory.setCredentialsRefreshService(refresh);
        };
    }

    private SSLContext contextoTls() throws Exception {
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (FileInputStream input = new FileInputStream(requerida("TRUSTSTORE_FILE"))) {
            store.load(input, requerida("TRUSTSTORE_PASSWORD").toCharArray());
        }
        if (store.size() == 0) {
            throw new IllegalStateException("TRUSTSTORE_FILE vacío");
        }
        TrustManagerFactory managers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        managers.init(store);
        SSLContext tls = SSLContext.getInstance("TLS");
        tls.init(null, managers.getTrustManagers(), null);
        return tls;
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
