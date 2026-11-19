package com.zerotrust.analyticsarchiver.config;

import com.zerotrust.analyticsarchiver.messaging.kafka.ConsumidorRawVotes;
import com.zerotrust.analyticsarchiver.messaging.kafka.LectorVotoCrudo;
import com.zerotrust.analyticsarchiver.repository.RepositorioAuditoriaJpa;
import com.zerotrust.analyticsarchiver.repository.VotoAuditadoJpaRepository;
import com.zerotrust.analyticsarchiver.port.RepositorioAuditoria;
import com.zerotrust.analyticsarchiver.service.ServicioArchivoVotos;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;

@Configuration
public class BeansArchivo {

    @Bean
    RepositorioAuditoria repositorioAuditoria(VotoAuditadoJpaRepository jpa) {
        return new RepositorioAuditoriaJpa(jpa);
    }

    @Bean
    ServicioArchivoVotos servicioArchivoVotos(RepositorioAuditoria repositorio) {
        return new ServicioArchivoVotos(repositorio);
    }

    @Bean
    ConsumidorRawVotes consumidorRawVotes(ServicioArchivoVotos servicio) {
        return new ConsumidorRawVotes(servicio, new LectorVotoCrudo());
    }

    @Bean
    CommonErrorHandler manejadorErroresKafka() {
        return new DefaultErrorHandler(PoliticaReintentos.mientrasFalleLaBaseDeDatos());
    }
}
