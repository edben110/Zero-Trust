package com.zerotrust.regionalrollup.config;

import com.zerotrust.regionalrollup.domain.model.RegionalVoteAccumulator;
import com.zerotrust.regionalrollup.domain.strategy.RegionalConsolidationStrategy;
import com.zerotrust.regionalrollup.domain.strategy.SimpleCountStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class AppConfig {

    @Bean
    public RegionalConsolidationStrategy consolidationStrategy() {
        // Estrategia activa. Para cambiar la lógica de consolidación, basta con
        // inyectar una implementación distinta de RegionalConsolidationStrategy aquí.
        return new SimpleCountStrategy();
    }

    @Bean
    public RegionalVoteAccumulator regionalVoteAccumulator(RegionalConsolidationStrategy strategy) {
        return new RegionalVoteAccumulator(strategy);
    }
}
