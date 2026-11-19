package com.zerotrust.voteprocessor.config;

import com.zerotrust.voteprocessor.adapter.memoria.ConteoEnMemoria;
import com.zerotrust.voteprocessor.adapter.persistence.ConteoVotosJdbc;
import com.zerotrust.voteprocessor.domain.model.VoteAggregate;
import com.zerotrust.voteprocessor.domain.port.ConteoVotos;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class AppConfig {

    static final String FUENTE_CONTEO = "processor.fuente-conteo";

    @Bean
    @ConditionalOnProperty(name = FUENTE_CONTEO, havingValue = "postgres", matchIfMissing = true)
    public ConteoVotos conteoVotosJdbc(JdbcTemplate jdbc) {
        return new ConteoVotosJdbc(jdbc);
    }

    @Bean
    @ConditionalOnProperty(name = FUENTE_CONTEO, havingValue = "memoria")
    public ConteoVotos conteoEnMemoria() {
        return new ConteoEnMemoria(new VoteAggregate());
    }
}
