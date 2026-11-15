package com.zerotrust.uservalidation.config;

import com.rabbitmq.client.impl.CredentialsProvider;
import com.rabbitmq.client.impl.CredentialsRefreshService;
import com.rabbitmq.client.impl.DefaultCredentialsRefreshService.DefaultCredentialsRefreshServiceBuilder;
import com.rabbitmq.client.impl.OAuth2ClientCredentialsGrantCredentialsProvider.OAuth2ClientCredentialsGrantCredentialsProviderBuilder;
import org.springframework.boot.autoconfigure.amqp.ConnectionFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;

// Sin usuario ni contraseña fijos: el token de auth_server viaja como contraseña y se renueva solo.
@Configuration
public class ConfiguracionRabbitOAuth {

    @Bean
    ConnectionFactoryCustomizer credencialesOAuthRabbit(VariablesClienteToken variables) {
        SSLContext tls = ContextoTls.desdeArchivo(variables.truststoreFile(), variables.truststorePassword());
        CredentialsProvider proveedor = new OAuth2ClientCredentialsGrantCredentialsProviderBuilder()
                .tokenEndpointUri(variables.authTokenUrl())
                .clientId(variables.clientId())
                .clientSecret(variables.clientSecret())
                .grantType("client_credentials")
                .tls().sslContext(tls).builder()
                .build();
        CredentialsRefreshService refresco = new DefaultCredentialsRefreshServiceBuilder().build();
        return fabrica -> {
            fabrica.setCredentialsProvider(proveedor);
            fabrica.setCredentialsRefreshService(refresco);
        };
    }
}
