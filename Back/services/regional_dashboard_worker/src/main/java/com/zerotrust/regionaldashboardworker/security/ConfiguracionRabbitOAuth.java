package com.zerotrust.regionaldashboardworker.security;

import com.rabbitmq.client.impl.CredentialsProvider;
import com.rabbitmq.client.impl.CredentialsRefreshService;
import com.rabbitmq.client.impl.DefaultCredentialsRefreshService.DefaultCredentialsRefreshServiceBuilder;
import com.rabbitmq.client.impl.OAuth2ClientCredentialsGrantCredentialsProvider.OAuth2ClientCredentialsGrantCredentialsProviderBuilder;
import org.springframework.boot.autoconfigure.amqp.ConnectionFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;

/** Usa un JWT renovable, emitido por vm-auth, como credencial AMQP. */
@Configuration
public class ConfiguracionRabbitOAuth {

    @Bean
    ConnectionFactoryCustomizer credencialesOAuthRabbit(Environment entorno) throws Exception {
        String endpoint = requerida(entorno, "AUTH_TOKEN_URL");
        String clientId = requerida(entorno, "CLIENT_ID");
        String clientSecret = requerida(entorno, "CLIENT_SECRET");
        String archivo = requerida(entorno, "TRUSTSTORE_FILE");
        String password = requerida(entorno, "TRUSTSTORE_PASSWORD");

        KeyStore truststore = KeyStore.getInstance("PKCS12");
        try (FileInputStream entrada = new FileInputStream(archivo)) {
            truststore.load(entrada, password.toCharArray());
        }
        if (truststore.size() == 0) {
            throw new IllegalStateException("TRUSTSTORE_FILE no contiene certificados de confianza");
        }
        TrustManagerFactory managers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        managers.init(truststore);
        SSLContext tls = SSLContext.getInstance("TLS");
        tls.init(null, managers.getTrustManagers(), null);

        CredentialsProvider proveedor = new OAuth2ClientCredentialsGrantCredentialsProviderBuilder()
                .tokenEndpointUri(endpoint)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType("client_credentials")
                .tls().sslContext(tls).builder()
                .build();
        CredentialsRefreshService refresco = new DefaultCredentialsRefreshServiceBuilder().build();
        return fabrica -> {
            fabrica.setCredentialsProvider(proveedor);
            fabrica.setCredentialsRefreshService(refresco);
        };
    }

    private static String requerida(Environment entorno, String nombre) {
        String valor = entorno.getProperty(nombre);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(nombre + ": variable obligatoria sin definir");
        }
        return valor;
    }
}
