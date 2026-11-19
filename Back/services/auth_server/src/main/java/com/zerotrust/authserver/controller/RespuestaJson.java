package com.zerotrust.authserver.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

// El cuerpo va ya serializado y con Content-Type fijado: así Spring no negocia con Accept (RF-31).
public final class RespuestaJson {

    private RespuestaJson() {
    }

    public static ResponseEntity.BodyBuilder sinCache(HttpStatusCode estado) {
        return ResponseEntity.status(estado)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache");
    }

    public static byte[] serializar(ObjectMapper json, Object cuerpo) {
        try {
            return json.writeValueAsBytes(cuerpo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("no se pudo serializar la respuesta", e);
        }
    }
}
