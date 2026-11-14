package com.zerotrust.uservalidation.adapter.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.uservalidation.domain.VoterId;

import java.util.Objects;
import java.util.Optional;

public class LectorRegistroElegible {

    private final ObjectMapper json = new ObjectMapper();

    // Optional vacío = registro malformado: se descarta sin detener el consumo.
    public Optional<CambioPadron> interpretar(String clave, String valor) {
        if (valor == null) {
            return voterId(clave).map(CambioPadron.Baja::new);
        }
        Optional<VoterId> delPayload = voterIdDelPayload(valor);
        if (delPayload.isEmpty()) {
            return Optional.empty();
        }
        if (clave != null && !Objects.equals(voterId(clave), delPayload)) {
            return Optional.empty();
        }
        return delPayload.map(CambioPadron.Alta::new);
    }

    private Optional<VoterId> voterIdDelPayload(String valor) {
        try {
            JsonNode nodo = json.readTree(valor);
            JsonNode voterId = nodo == null ? null : nodo.get("voterId");
            return voterId != null && voterId.isTextual() ? voterId(voterId.textValue()) : Optional.empty();
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    private static Optional<VoterId> voterId(String texto) {
        try {
            return Optional.of(new VoterId(texto));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
