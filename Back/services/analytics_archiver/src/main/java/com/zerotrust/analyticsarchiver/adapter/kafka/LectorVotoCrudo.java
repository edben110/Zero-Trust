package com.zerotrust.analyticsarchiver.adapter.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.analyticsarchiver.domain.CandidateId;
import com.zerotrust.analyticsarchiver.domain.Region;
import com.zerotrust.analyticsarchiver.domain.SourceIp;
import com.zerotrust.analyticsarchiver.domain.VoteId;
import com.zerotrust.analyticsarchiver.domain.VoterId;
import com.zerotrust.analyticsarchiver.domain.VotoAuditado;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public class LectorVotoCrudo {

    private final ObjectMapper json = new ObjectMapper();

    // Optional vacío = mensaje malformado: se descarta sin detener el consumo.
    public Optional<VotoAuditado> interpretar(String clave, String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        try {
            JsonNode nodo = json.readTree(valor);
            if (nodo == null || !nodo.isObject()) {
                return Optional.empty();
            }
            VotoAuditado voto = new VotoAuditado(new VoteId(texto(nodo, "voteId")), new VoterId(texto(nodo, "voterId")),
                    new CandidateId(texto(nodo, "candidateId")), new Region(texto(nodo, "region")),
                    new SourceIp(texto(nodo, "sourceIp")), Instant.parse(texto(nodo, "timestamp")));
            if (clave != null && !clave.equalsIgnoreCase(voto.voteId().valor())) {
                return Optional.empty();
            }
            return Optional.of(voto);
        } catch (IOException | IllegalArgumentException | NullPointerException | DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private static String texto(JsonNode nodo, String campo) {
        JsonNode valor = nodo.get(campo);
        return valor != null && valor.isTextual() ? valor.textValue() : null;
    }
}
