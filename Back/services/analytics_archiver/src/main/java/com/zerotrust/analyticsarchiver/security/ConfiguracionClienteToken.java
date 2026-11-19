package com.zerotrust.analyticsarchiver.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class ConfiguracionClienteToken {

    @Bean
    VariablesClienteToken variablesClienteToken(Environment entorno) {
        return VariablesClienteToken.desde(entorno::getProperty);
    }
}
