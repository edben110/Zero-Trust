package com.zerotrust.voteprocessor.config;

import org.springframework.amqp.core.FanoutExchange;
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
}
