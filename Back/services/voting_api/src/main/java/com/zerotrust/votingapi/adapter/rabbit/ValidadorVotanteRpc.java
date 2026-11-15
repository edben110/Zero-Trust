package com.zerotrust.votingapi.adapter.rabbit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.votingapi.domain.ValidacionNoDisponibleException;
import com.zerotrust.votingapi.domain.VoterId;
import com.zerotrust.votingapi.port.ValidadorVotante;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.IOException;
import java.util.Map;

// RPC sobre RabbitMQ: RabbitTemplate pone reply-to y correlationId; el timeout es spring.rabbitmq.template.reply-timeout.
public class ValidadorVotanteRpc implements ValidadorVotante {

    private final RabbitTemplate plantilla;
    private final String cola;
    private final ObjectMapper json;

    public ValidadorVotanteRpc(RabbitTemplate plantilla, String cola, ObjectMapper json) {
        this.plantilla = plantilla;
        this.cola = cola;
        this.json = json;
    }

    @Override
    public boolean estaHabilitado(VoterId voterId) {
        Message respuesta;
        try {
            respuesta = plantilla.sendAndReceive("", cola, peticion(voterId));
        } catch (AmqpException e) {
            throw new ValidacionNoDisponibleException();
        }
        if (respuesta == null) {
            throw new ValidacionNoDisponibleException();
        }
        return leer(respuesta.getBody());
    }

    private Message peticion(VoterId voterId) {
        try {
            byte[] cuerpo = json.writeValueAsBytes(Map.of("voterId", voterId.valor()));
            return MessageBuilder.withBody(cuerpo).setContentType(MessageProperties.CONTENT_TYPE_JSON).build();
        } catch (IOException e) {
            throw new IllegalStateException("no se pudo serializar la petición", e);
        }
    }

    // Una respuesta que no es {"valid": <booleano>} nunca se toma como positiva.
    private boolean leer(byte[] cuerpo) {
        try {
            JsonNode nodo = json.readTree(cuerpo);
            JsonNode valido = nodo == null ? null : nodo.get("valid");
            if (valido == null || !valido.isBoolean()) {
                throw new ValidacionNoDisponibleException();
            }
            return valido.booleanValue();
        } catch (IOException e) {
            throw new ValidacionNoDisponibleException();
        }
    }
}
