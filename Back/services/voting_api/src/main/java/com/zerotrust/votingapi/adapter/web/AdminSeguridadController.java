package com.zerotrust.votingapi.adapter.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.votingapi.domain.DireccionIp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Funciones de seguridad del panel de administración.
 *
 * - POST /drills: lanza una prueba controlada. Publica en security_drills y el
 *   bot_detector_service ejecuta el MISMO patrón State que usa con tráfico real.
 *   No se escribe en raw_votes: el escrutinio y la auditoría electoral quedan
 *   intactos.
 * - POST /reset: libera una IP BLOQUEADA (estado terminal del patrón State).
 * - POST /purge: purga el histórico de incidentes y el estado vigente por IP.
 *   También ordena al detector olvidar sus contextos en memoria.
 * - GET  /incidents: lee de PostgreSQL el estado vigente por IP y el histórico
 *   de fraud_incidents (append-only salvo la purga administrativa explícita).
 *
 * Solo lo invoca el BFF autenticado con el token compartido. No usa CORS.
 */
@RestController
@RequestMapping("/internal/admin/security")
public final class AdminSeguridadController {

    private static final int MAX_VOTOS_POR_PRUEBA = 50;
    private static final int MAX_INCIDENTES = 200;
    private static final int MAX_ESTADOS = 100;
    private static final long ESPERA_KAFKA_SEGUNDOS = 5;

    private final byte[] token;
    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;
    private final String topic;
    private final int suspiciousThreshold;
    private final int blockedThreshold;
    private final int windowSeconds;

    public AdminSeguridadController(
            @Value("${ADMIN_API_TOKEN}") String clave,
            JdbcTemplate jdbc,
            KafkaTemplate<String, String> kafka,
            ObjectMapper json,
            @Value("${seguridad.drills-topic:security_drills}") String topic,
            @Value("${seguridad.suspicious-threshold:5}") int suspiciousThreshold,
            @Value("${seguridad.blocked-threshold:10}") int blockedThreshold,
            @Value("${seguridad.window-seconds:60}") int windowSeconds) {
        if (clave == null || clave.length() < 32) {
            throw new IllegalStateException("ADMIN_API_TOKEN requiere al menos 32 caracteres");
        }
        if (blockedThreshold <= suspiciousThreshold || suspiciousThreshold <= 0) {
            throw new IllegalStateException("Los umbrales de seguridad no son coherentes");
        }
        this.token               = clave.getBytes(StandardCharsets.UTF_8);
        this.jdbc                = jdbc;
        this.kafka               = kafka;
        this.json                = json;
        this.topic               = topic;
        this.suspiciousThreshold = suspiciousThreshold;
        this.blockedThreshold    = blockedThreshold;
        this.windowSeconds       = windowSeconds;
    }

    public record SolicitudPrueba(String ip, Integer votes) { }

    public record SolicitudReinicio(String ip) { }

    @PostMapping("/drills")
    public ResponseEntity<?> simular(
            @RequestHeader(value = "X-Admin-Token", required = false) String recibido,
            @RequestBody(required = false) SolicitudPrueba datos) {
        if (!autorizado(recibido)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (datos == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Indique la IP y el número de votos."));
        }
        final DireccionIp ip;
        final int votos;
        try {
            ip = new DireccionIp(datos.ip());
            votos = datos.votes() == null ? 1 : datos.votes();
            if (votos < 1 || votos > MAX_VOTOS_POR_PRUEBA) {
                return ResponseEntity.badRequest().body(Map.of("error",
                        "El número de votos debe estar entre 1 y " + MAX_VOTOS_POR_PRUEBA + "."));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }

        try {
            for (int i = 0; i < votos; i++) {
                publicar(ip.valor(), "EVALUATE", 1);
            }
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of("accepted", true, "ip", ip.valor(), "votes", votos, "topic", topic));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "No se pudo publicar la prueba en Kafka."));
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<?> reiniciar(
            @RequestHeader(value = "X-Admin-Token", required = false) String recibido,
            @RequestBody(required = false) SolicitudReinicio datos) {
        if (!autorizado(recibido)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        final DireccionIp ip;
        try {
            ip = new DireccionIp(datos == null ? null : datos.ip());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
        try {
            publicar(ip.valor(), "RESET", 0);
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .body(Map.of("reset", true, "ip", ip.valor()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "No se pudo publicar el reinicio en Kafka."));
        }
    }

    /**
     * Purga administrativa: elimina el histórico de incidentes y el estado
     * vigente por IP. Primero ordena al detector olvidar sus contextos en
     * memoria; si Kafka no confirma, no se borra nada para no dejar el panel
     * mostrando ceros mientras el detector sigue contando votos.
     *
     * No toca el padrón, los votos aceptados ni los intentos de región: la
     * jornada electoral sigue siendo la misma.
     */
    @PostMapping("/purge")
    public ResponseEntity<?> purgar(
            @RequestHeader(value = "X-Admin-Token", required = false) String recibido) {
        if (!autorizado(recibido)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            publicarComando("RESET_ALL");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "No se pudo reiniciar el detector en Kafka; no se borraron registros."));
        }
        try {
            int incidentes = jdbc.update("DELETE FROM fraud_incidents");
            int estados = jdbc.update("DELETE FROM ip_risk_state");
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .body(Map.of("purged", true, "incidents", incidentes, "ips", estados));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "El detector se reinició, pero no se pudo purgar PostgreSQL."));
        }
    }

    @GetMapping("/incidents")
    public ResponseEntity<?> incidentes(
            @RequestHeader(value = "X-Admin-Token", required = false) String recibido) {
        if (!autorizado(recibido)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            Map<String, Object> respuesta = new LinkedHashMap<>();
            respuesta.put("thresholds", Map.of(
                    "suspicious", suspiciousThreshold,
                    "blocked", blockedThreshold,
                    "windowSeconds", windowSeconds));
            respuesta.put("summary", resumen());
            respuesta.put("ips", estados());
            respuesta.put("incidents", incidentesRecientes());
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(respuesta);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "No se pudo consultar PostgreSQL."));
        }
    }

    private void publicar(String ip, String accion, int votos) throws Exception {
        Map<String, Object> mensaje = new LinkedHashMap<>();
        mensaje.put("action", accion);
        mensaje.put("ip", ip);
        mensaje.put("votes", votos);
        String carga = json.writeValueAsString(mensaje);
        // Esperar la confirmación del broker: el panel no debe decir "enviado"
        // si Kafka rechazó el mensaje (ACL, broker caído, etc.).
        kafka.send(topic, ip, carga).get(ESPERA_KAFKA_SEGUNDOS, TimeUnit.SECONDS);
    }

    private void publicarComando(String accion) throws Exception {
        Map<String, Object> mensaje = new LinkedHashMap<>();
        mensaje.put("action", accion);
        String carga = json.writeValueAsString(mensaje);
        kafka.send(topic, "admin", carga).get(ESPERA_KAFKA_SEGUNDOS, TimeUnit.SECONDS);
    }

    private Map<String, Object> resumen() {
        Map<String, Object> totales = jdbc.queryForObject("""
                SELECT count(*)                                          AS total,
                       count(*) FILTER (WHERE origin = 'LIVE')           AS live,
                       count(*) FILTER (WHERE origin = 'DRILL')          AS drills,
                       count(*) FILTER (WHERE kind = 'DUPLICATE_VOTE')   AS duplicados,
                       count(*) FILTER (WHERE kind = 'WRONG_REGION')     AS region,
                       count(DISTINCT ip)                                AS ips
                FROM fraud_incidents
                """, (rs, fila) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("incidents", rs.getLong("total"));
            m.put("live", rs.getLong("live"));
            m.put("drills", rs.getLong("drills"));
            m.put("duplicates", rs.getLong("duplicados"));
            m.put("wrongRegion", rs.getLong("region"));
            m.put("ipsWithIncidents", rs.getLong("ips"));
            return m;
        });
        if (totales == null) {
            totales = new LinkedHashMap<>();
        }
        Map<String, Long> porNivel = new LinkedHashMap<>();
        porNivel.put("NORMAL", 0L);
        porNivel.put("SOSPECHOSA", 0L);
        porNivel.put("BLOQUEADA", 0L);
        jdbc.query("SELECT risk_level, count(*) AS total FROM ip_risk_state GROUP BY risk_level",
                (ResultSet rs) -> {
                    porNivel.put(rs.getString("risk_level"), rs.getLong("total"));
                });
        totales.put("currentRisk", porNivel);
        return totales;
    }

    private List<Map<String, Object>> estados() {
        return jdbc.query("""
                SELECT ip, risk_level, vote_count, window_seconds, first_seen, last_seen
                FROM ip_risk_state
                ORDER BY last_seen DESC
                LIMIT ?
                """, (rs, fila) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ip", rs.getString("ip"));
            m.put("riskLevel", rs.getString("risk_level"));
            m.put("voteCount", rs.getInt("vote_count"));
            m.put("windowSeconds", rs.getInt("window_seconds"));
            m.put("firstSeen", instante(rs, "first_seen"));
            m.put("lastSeen", instante(rs, "last_seen"));
            return m;
        }, MAX_ESTADOS);
    }

    private List<Map<String, Object>> incidentesRecientes() {
        return jdbc.query("""
                SELECT id, ip, kind, risk_level, vote_count, window_seconds, origin, detected_at
                FROM fraud_incidents
                ORDER BY detected_at DESC, id DESC
                LIMIT ?
                """, (rs, fila) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", rs.getLong("id"));
            m.put("ip", rs.getString("ip"));
            m.put("kind", rs.getString("kind"));
            m.put("riskLevel", rs.getString("risk_level"));
            m.put("voteCount", rs.getInt("vote_count"));
            m.put("windowSeconds", rs.getInt("window_seconds"));
            m.put("origin", rs.getString("origin"));
            m.put("detectedAt", instante(rs, "detected_at"));
            return m;
        }, MAX_INCIDENTES);
    }

    private static String instante(ResultSet rs, String columna) throws SQLException {
        var marca = rs.getTimestamp(columna);
        return marca == null ? null : marca.toInstant().toString();
    }

    private boolean autorizado(String recibido) {
        return recibido != null && MessageDigest.isEqual(token, recibido.getBytes(StandardCharsets.UTF_8));
    }
}
