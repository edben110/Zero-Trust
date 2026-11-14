package com.zerotrust.regionaldashboardworker.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange liveResultsRegionalExchange(
            @Value("${rabbit.exchange.live-results-regional:live_results_regional}") String name) {
        return new TopicExchange(name, true, false);
    }

    @Bean
    public Queue regionalDashboardQueue(
            @Value("${dashboard.region-binding:#}") String regionPattern) {
        // Cola exclusiva por instancia del worker.
        // Con regionPattern "#" escucha todas las regiones (comportamiento por defecto).
        return new Queue("", false, true, true);
    }

    @Bean
    public Binding regionalBinding(
            Queue regionalDashboardQueue,
            TopicExchange liveResultsRegionalExchange,
            @Value("${dashboard.region-binding:#}") String pattern) {
        // La routing key sigue el contrato: results.<region>
        // Con pattern "results.#" este worker recibe todas las regiones.
        // Para un worker dedicado a una sola región: "results.norte"
        return BindingBuilder
                .bind(regionalDashboardQueue)
                .to(liveResultsRegionalExchange)
                .with("results." + pattern);
    }
}
