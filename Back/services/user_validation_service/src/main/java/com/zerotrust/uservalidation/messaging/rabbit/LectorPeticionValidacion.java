package com.zerotrust.uservalidation.messaging.rabbit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.uservalidation.domain.VoterId;

import java.io.IOException;
import java.util.Optional;

public class LectorPeticionValidacion {

    private final ObjectMapper json = new ObjectMapper();

    public Optional<VoterId> leer(byte[] cuerpo) {
        try {
            JsonNode nodo = json.readTree(cuerpo);
            JsonNode voterId = nodo == null ? null : nodo.get("voterId");
            if (voterId == null || !voterId.isTextual()) {
                return Optional.empty();
            }
            return Optional.of(new VoterId(voterId.textValue()));
        } catch (IOException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
