package com.zerotrust.uservalidation.adapter.rabbit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.uservalidation.service.ServicioPadronElectoral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.util.Map;

// Spring AMQP envía el Message devuelto al reply-to y copia el correlationId de la petición.
public class ListenerValidacionRpc {

    private static final Logger LOG = LoggerFactory.getLogger(ListenerValidacionRpc.class);

    private final ServicioPadronElectoral servicio;
    private final LectorPeticionValidacion lector;
    private final ObjectMapper json;

    public ListenerValidacionRpc(ServicioPadronElectoral servicio, LectorPeticionValidacion lector, ObjectMapper json) {
        this.servicio = servicio;
        this.lector = lector;
        this.json = json;
    }

    @RabbitListener(queues = "${validacion.cola}")
    public Message responder(Message peticion) {
        boolean valido = lector.leer(peticion.getBody())
                .map(servicio::esElegible)
                .orElseGet(() -> {
                    LOG.warn("Petición de validación malformada: se responde valid=false");
                    return false;
                });
        return MessageBuilder.withBody(cuerpo(valido))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .build();
    }

    private byte[] cuerpo(boolean valido) {
        try {
            return json.writeValueAsBytes(Map.of("valid", valido));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("no se pudo serializar la respuesta", e);
        }
    }
}
