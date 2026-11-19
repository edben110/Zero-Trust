package com.zerotrust.votingapi.controller;

import com.zerotrust.votingapi.dto.request.VotanteRequest;
import com.zerotrust.votingapi.service.ServicioPadron;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

// Solo la llama el BFF con el token compartido; no usa CORS y la respuesta no incluye la cédula ni el UUID.
@RestController
@RequestMapping("/internal/admin/voters")
public class AdminVotantesController {

    private final ServicioPadron servicio;
    private final byte[] token;

    public AdminVotantesController(ServicioPadron servicio, @Value("${ADMIN_API_TOKEN}") String token) {
        if (token == null || token.length() < 32) {
            throw new IllegalStateException("ADMIN_API_TOKEN requiere al menos 32 caracteres");
        }
        this.servicio = servicio;
        this.token = token.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping
    public ResponseEntity<?> inscribir(@RequestHeader(value = "X-Admin-Token", required = false) String clave,
                                       @RequestBody(required = false) VotanteRequest datos) {
        if (!autorizado(clave)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (datos == null) {
            datos = new VotanteRequest(null, null);
        }
        servicio.inscribir(datos.cedulaTexto(), datos.region());
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(Map.of("registered", true));
    }

    private boolean autorizado(String recibido) {
        return recibido != null && MessageDigest.isEqual(token, recibido.getBytes(StandardCharsets.UTF_8));
    }
}
