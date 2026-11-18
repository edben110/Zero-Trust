package com.zerotrust.globaldashboardworker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final String[] allowedOrigins;

    public WebSocketConfig(@Value("${DASHBOARD_ALLOWED_ORIGINS}") String origins) {
        this.allowedOrigins = java.util.Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
        if (allowedOrigins.length == 0 || java.util.Arrays.stream(allowedOrigins)
                .anyMatch(origin -> origin.contains("*") || !origin.matches("https?://[^/]+"))) {
            throw new IllegalArgumentException("DASHBOARD_ALLOWED_ORIGINS requiere orígenes explícitos");
        }
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Destinos donde el servidor publica mensajes a clientes web
        registry.enableSimpleBroker("/topic");
        // Prefijo para mensajes del cliente hacia el servidor (no utilizado aquí)
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint de conexión STOMP para el navegador
        registry.addEndpoint("/ws/results")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();
    }
}
