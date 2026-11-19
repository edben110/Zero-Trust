package com.zerotrust.voterregistration.security;

import org.springframework.boot.autoconfigure.kafka.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class ConfiguracionKafkaOAuth {

    @Bean
    DefaultKafkaConsumerFactoryCustomizer seguridadConsumidorKafka(VariablesClienteToken variables) {
        Map<String, Object> propiedades = PropiedadesKafkaOAuth.propiedades(variables);
        return fabrica -> fabrica.updateConfigs(propiedades);
    }

    @Bean
    DefaultKafkaProducerFactoryCustomizer seguridadProductorKafka(VariablesClienteToken variables) {
        Map<String, Object> propiedades = PropiedadesKafkaOAuth.propiedades(variables);
        return fabrica -> fabrica.updateConfigs(propiedades);
    }
}
