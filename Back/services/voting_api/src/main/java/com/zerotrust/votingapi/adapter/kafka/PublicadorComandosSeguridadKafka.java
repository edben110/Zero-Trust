package com.zerotrust.votingapi.adapter.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.votingapi.domain.AccionSeguridad;
import com.zerotrust.votingapi.exception.CanalSeguridadNoDisponibleException;
import com.zerotrust.votingapi.domain.DireccionIp;
import com.zerotrust.votingapi.port.PublicadorComandosSeguridad;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publica los comandos del panel en security_drills con el formato de docs/contratos.md.
 */
public class PublicadorComandosSeguridadKafka implements PublicadorComandosSeguridad {

    private static final String CLAVE_ADMIN = "admin";

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;
    private final String topic;
    private final Duration espera;

    public PublicadorComandosSeguridadKafka(KafkaTemplate<String, String> kafka, ObjectMapper json,
                                            String topic, Duration espera) {
        this.kafka = kafka;
        this.json = json;
        this.topic = topic;
        this.espera = espera;
    }

    @Override
    public void evaluar(DireccionIp ip) {
        enviar(ip.valor(), comandoParaIp(AccionSeguridad.EVALUATE, ip, 1));
    }

    @Override
    public void reiniciar(DireccionIp ip) {
        enviar(ip.valor(), comandoParaIp(AccionSeguridad.RESET, ip, 0));
    }

    @Override
    public void reiniciarTodo() {
        enviar(CLAVE_ADMIN, Map.of("action", AccionSeguridad.RESET_ALL.name()));
    }

    @Override
    public String destino() {
        return topic;
    }

    private static Map<String, Object> comandoParaIp(AccionSeguridad accion, DireccionIp ip, int votos) {
        Map<String, Object> comando = new LinkedHashMap<>();
        comando.put("action", accion.name());
        comando.put("ip", ip.valor());
        comando.put("votes", votos);
        return comando;
    }

    // Se espera la confirmación: el panel no debe decir "enviado" si Kafka rechazó el mensaje (ACL, broker caído).
    private void enviar(String clave, Map<String, Object> comando) {
        try {
            kafka.send(topic, clave, json.writeValueAsString(comando)).get(espera.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CanalSeguridadNoDisponibleException("Envío a " + topic + " interrumpido", e);
        } catch (ExecutionException | TimeoutException | JsonProcessingException | RuntimeException e) {
            throw new CanalSeguridadNoDisponibleException("Kafka no confirmó el envío a " + topic, e);
        }
    }
}
