package com.zerotrust.votingapi.controller;

import com.zerotrust.votingapi.config.RepositorioCatalogo;
import com.zerotrust.votingapi.domain.ElectionId;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cambia la jornada activa sin borrar el padrón, los candidatos ni la auditoría. */
@RestController
@RequestMapping("/internal/admin/election")
public final class AdminEleccionController {
    private final byte[] token;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaccion;
    private final ElectionId eleccion;
    private final RepositorioCatalogo catalogo;

    public AdminEleccionController(@Value("${ADMIN_API_TOKEN}") String clave,
            JdbcTemplate jdbc, PlatformTransactionManager gestor, ElectionId eleccion,
            RepositorioCatalogo catalogo) {
        if (clave == null || clave.length() < 32) throw new IllegalStateException("ADMIN_API_TOKEN no configurado");
        this.token = clave.getBytes(StandardCharsets.UTF_8);
        this.jdbc = jdbc;
        this.transaccion = new TransactionTemplate(gestor);
        this.eleccion = eleccion;
        this.catalogo = catalogo;
    }

    @PostMapping("/reset")
    public ResponseEntity<?> reiniciar(@RequestHeader(value = "X-Admin-Token", required = false) String recibido) {
        if (recibido == null || !MessageDigest.isEqual(token, recibido.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (catalogo.actual().candidatos().size() < 2) {
            return ResponseEntity.badRequest().body(Map.of("error", "Se requieren al menos dos candidatos."));
        }
        // La actualización espera a cualquier voto con bloqueo FOR SHARE y bloquea
        // nuevos votos hasta vaciar el registro de uso de esta elección.
        transaccion.executeWithoutResult(estado -> {
            int actualizadas = jdbc.update("UPDATE election_state SET run_id=? WHERE id=1",
                    UUID.randomUUID());
            if (actualizadas != 1) throw new IllegalStateException("No existe elección activa");
            jdbc.update("DELETE FROM voter_election_usage WHERE election_id=?", eleccion.valor());
        });
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(Map.of("reset", true));
    }
}
