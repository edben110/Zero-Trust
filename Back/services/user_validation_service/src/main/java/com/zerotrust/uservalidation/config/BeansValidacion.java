package com.zerotrust.uservalidation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.uservalidation.adapter.kafka.ConsumidorEligibleVoters;
import com.zerotrust.uservalidation.adapter.kafka.LectorRegistroElegible;
import com.zerotrust.uservalidation.adapter.memoria.PadronEnMemoria;
import com.zerotrust.uservalidation.adapter.rabbit.LectorPeticionValidacion;
import com.zerotrust.uservalidation.adapter.rabbit.ListenerValidacionRpc;
import com.zerotrust.uservalidation.domain.PadronElectoral;
import com.zerotrust.uservalidation.port.PadronVotantes;
import com.zerotrust.uservalidation.service.ServicioPadronElectoral;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeansValidacion {

    @Bean
    PadronVotantes padronVotantes() {
        return new PadronEnMemoria(new PadronElectoral());
    }

    @Bean
    ServicioPadronElectoral servicioPadronElectoral(PadronVotantes padron) {
        return new ServicioPadronElectoral(padron);
    }

    @Bean
    ConsumidorEligibleVoters consumidorEligibleVoters(ServicioPadronElectoral servicio) {
        return new ConsumidorEligibleVoters(servicio, new LectorRegistroElegible());
    }

    @Bean
    ListenerValidacionRpc listenerValidacionRpc(ServicioPadronElectoral servicio) {
        return new ListenerValidacionRpc(servicio, new LectorPeticionValidacion(), new ObjectMapper());
    }

    // Durable, no exclusiva y sin borrado automático: las réplicas compiten por sus mensajes.
    @Bean
    Queue colaValidacion(@Value("${validacion.cola}") String nombre) {
        return new Queue(nombre, true, false, false);
    }
}
