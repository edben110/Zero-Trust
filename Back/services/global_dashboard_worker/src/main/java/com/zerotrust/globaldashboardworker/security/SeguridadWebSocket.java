package com.zerotrust.globaldashboardworker.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** El resultado público no exige login; el resto conserva autenticación JWT. */
@Configuration
public class SeguridadWebSocket {

    @Bean
    SecurityFilterChain seguridadResultados(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/ws/results", "/ws/results/**").permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/ws/results", "/ws/results/**"))
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> { }))
                .build();
    }
}
