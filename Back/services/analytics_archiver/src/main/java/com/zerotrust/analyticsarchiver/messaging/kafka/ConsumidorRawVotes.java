package com.zerotrust.analyticsarchiver.messaging.kafka;

import com.zerotrust.analyticsarchiver.service.ServicioArchivoVotos;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

public class ConsumidorRawVotes {

    private static final Logger LOG = LoggerFactory.getLogger(ConsumidorRawVotes.class);

    private final ServicioArchivoVotos servicio;
    private final LectorVotoCrudo lector;

    public ConsumidorRawVotes(ServicioArchivoVotos servicio, LectorVotoCrudo lector) {
        this.servicio = servicio;
        this.lector = lector;
    }

    // Un fallo de la base de datos se propaga: el error handler reintenta sin confirmar el offset.
    @KafkaListener(topics = "${archivo.topic}")
    public void recibir(ConsumerRecord<String, String> registro) {
        lector.interpretar(registro.key(), registro.value()).ifPresentOrElse(
                voto -> {
                    if (!servicio.archivar(voto)) {
                        LOG.info("Voto ya archivado, se omite (partición {}, offset {})",
                                registro.partition(), registro.offset());
                    }
                },
                () -> LOG.warn("Mensaje de raw_votes descartado por malformado (partición {}, offset {})",
                        registro.partition(), registro.offset()));
    }
}
