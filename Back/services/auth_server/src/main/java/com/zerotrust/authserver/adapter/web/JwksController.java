package com.zerotrust.authserver.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.authserver.service.ServicioJwks;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JwksController {

    private final ServicioJwks servicio;
    private final ObjectMapper json;

    public JwksController(ServicioJwks servicio, ObjectMapper json) {
        this.servicio = servicio;
        this.json = json;
    }

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<byte[]> jwks() {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(RespuestaJson.serializar(json, servicio.jwkSet()));
    }
}
