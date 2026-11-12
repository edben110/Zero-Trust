package com.zerotrust.globaldashboardworker.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public FanoutExchange liveResultsGlobalExchange(
            @Value("${rabbit.exchange.live-results-global:live_results_global}") String name) {
        return new FanoutExchange(name, true, false);
    }

    @Bean
    public Queue globalDashboardQueue() {
        // Cola exclusiva por instancia del worker
        return new Queue("", false, true, true);
    }

    @Bean
    public Binding globalBinding(Queue globalDashboardQueue, FanoutExchange liveResultsGlobalExchange) {
        return BindingBuilder.bind(globalDashboardQueue).to(liveResultsGlobalExchange);
    }
}
