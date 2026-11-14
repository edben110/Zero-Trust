package com.zerotrust.voteprocessor.config;

import com.zerotrust.voteprocessor.domain.model.VoteAggregate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class AppConfig {

    // VoteAggregate es un bean singleton: todo el estado vive en la misma instancia.
    @Bean
    public VoteAggregate voteAggregate() {
        return new VoteAggregate();
    }
}
