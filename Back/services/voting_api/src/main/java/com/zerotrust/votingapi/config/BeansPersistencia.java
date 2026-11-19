package com.zerotrust.votingapi.config;

import com.zerotrust.votingapi.port.ControlRegionVotante;
import com.zerotrust.votingapi.port.EjecutorTransaccional;
import com.zerotrust.votingapi.port.RegistroParticipacion;
import com.zerotrust.votingapi.port.RegistroVotoAceptado;
import com.zerotrust.votingapi.port.RepositorioPadron;
import com.zerotrust.votingapi.repository.ControlRegionVotanteJdbc;
import com.zerotrust.votingapi.repository.EjecutorTransaccionalSpring;
import com.zerotrust.votingapi.repository.RegistroParticipacionJdbc;
import com.zerotrust.votingapi.repository.RegistroVotoAceptadoJdbc;
import com.zerotrust.votingapi.repository.RepositorioPadronJdbc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Adaptadores de PostgreSQL de la votación y el padrón.
 */
@Configuration
public class BeansPersistencia {

    @Bean
    RepositorioPadron repositorioPadron(JdbcTemplate jdbc) {
        return new RepositorioPadronJdbc(jdbc);
    }

    @Bean
    RegistroParticipacion registroParticipacion(JdbcTemplate jdbc) {
        return new RegistroParticipacionJdbc(jdbc);
    }

    @Bean
    RegistroVotoAceptado registroVotoAceptado(JdbcTemplate jdbc) {
        return new RegistroVotoAceptadoJdbc(jdbc);
    }

    @Bean
    ControlRegionVotante controlRegionVotante(JdbcTemplate jdbc) {
        return new ControlRegionVotanteJdbc(jdbc);
    }

    @Bean
    EjecutorTransaccional ejecutorTransaccional(PlatformTransactionManager gestor) {
        return new EjecutorTransaccionalSpring(gestor);
    }
}
