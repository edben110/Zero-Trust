package com.zerotrust.uservalidation.messaging.kafka;

import com.zerotrust.uservalidation.service.ServicioPadronElectoral;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

public class ConsumidorEligibleVoters {

    private static final Logger LOG = LoggerFactory.getLogger(ConsumidorEligibleVoters.class);

    private final ServicioPadronElectoral servicio;
    private final LectorRegistroElegible lector;

    public ConsumidorEligibleVoters(ServicioPadronElectoral servicio, LectorRegistroElegible lector) {
        this.servicio = servicio;
        this.lector = lector;
    }

    @KafkaListener(topics = "${padron.topic}")
    public void recibir(ConsumerRecord<String, String> registro) {
        lector.interpretar(registro.key(), registro.value()).ifPresentOrElse(
                cambio -> cambio.aplicarEn(servicio),
                () -> LOG.warn("Registro de eligible_voters descartado por malformado (partición {}, offset {})",
                        registro.partition(), registro.offset()));
    }
}
