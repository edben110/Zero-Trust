package com.zerotrust.votingapi.controller;

import com.zerotrust.votingapi.config.RepositorioCatalogo;
import com.zerotrust.votingapi.dto.request.CandidatoRequest;
import com.zerotrust.votingapi.dto.response.CatalogoResponse;
import com.zerotrust.votingapi.mapper.MapperCatalogo;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Solo el BFF autenticado puede modificar el catálogo. No se usa CORS. */
@RestController
@RequestMapping("/internal/admin/candidates")
public class AdminCandidatosController {
    private final RepositorioCatalogo repositorio;
    private final byte[] token;

    public AdminCandidatosController(RepositorioCatalogo repositorio,
            @Value("${ADMIN_API_TOKEN}") String token) {
        if (token == null || token.length() < 32) {
            throw new IllegalStateException("ADMIN_API_TOKEN requiere al menos 32 caracteres");
        }
        this.repositorio = repositorio;
        this.token = token.getBytes(StandardCharsets.UTF_8);
    }

    private boolean autorizado(String recibido) {
        return recibido != null && MessageDigest.isEqual(token, recibido.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestHeader(value = "X-Admin-Token", required = false) String clave,
            @RequestBody(required = false) CandidatoRequest datos) {
        if (!autorizado(clave)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ejecutar(() -> MapperCatalogo.aResponse(repositorio.crear(datos.name(), datos.list())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@RequestHeader(value = "X-Admin-Token", required = false) String clave,
            @PathVariable String id, @RequestBody(required = false) CandidatoRequest datos) {
        if (!autorizado(clave)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ejecutar(() -> MapperCatalogo.aResponse(repositorio.editar(id, datos.name(), datos.list())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@RequestHeader(value = "X-Admin-Token", required = false) String clave,
            @PathVariable String id) {
        if (!autorizado(clave)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ejecutar(() -> MapperCatalogo.aResponse(repositorio.eliminar(id)));
    }

    private ResponseEntity<?> ejecutar(java.util.function.Supplier<CatalogoResponse> operacion) {
        try {
            return ResponseEntity.ok(operacion.get());
        } catch (IllegalArgumentException | NullPointerException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error",
                    e.getMessage() == null ? "Datos inválidos." : e.getMessage()));
        }
    }
}
