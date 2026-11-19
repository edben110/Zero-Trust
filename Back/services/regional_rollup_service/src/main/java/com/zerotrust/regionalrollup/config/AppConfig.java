package com.zerotrust.regionalrollup.config;

import com.zerotrust.regionalrollup.adapter.memoria.ConteoRegionalEnMemoria;
import com.zerotrust.regionalrollup.adapter.persistence.ConteoVotosRegionalesJdbc;
import com.zerotrust.regionalrollup.domain.model.RegionalVoteAccumulator;
import com.zerotrust.regionalrollup.domain.port.ConteoVotosRegionales;
import com.zerotrust.regionalrollup.domain.strategy.RegionalConsolidationStrategy;
import com.zerotrust.regionalrollup.domain.strategy.SimpleCountStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class AppConfig {

    static final String FUENTE_CONTEO = "rollup.fuente-conteo";

    @Bean
    public RegionalConsolidationStrategy consolidationStrategy() {
        // Estrategia activa. Para cambiar la lógica de consolidación, basta con
        // inyectar una implementación distinta de RegionalConsolidationStrategy aquí.
        return new SimpleCountStrategy();
    }

    @Bean
    @ConditionalOnProperty(name = FUENTE_CONTEO, havingValue = "postgres", matchIfMissing = true)
    public ConteoVotosRegionales conteoVotosRegionalesJdbc(JdbcTemplate jdbc) {
        return new ConteoVotosRegionalesJdbc(jdbc);
    }

    @Bean
    @ConditionalOnProperty(name = FUENTE_CONTEO, havingValue = "memoria")
    public ConteoVotosRegionales conteoRegionalEnMemoria(RegionalConsolidationStrategy strategy) {
        return new ConteoRegionalEnMemoria(new RegionalVoteAccumulator(strategy));
    }
}
