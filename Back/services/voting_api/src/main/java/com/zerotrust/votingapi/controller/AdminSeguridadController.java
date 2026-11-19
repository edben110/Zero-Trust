package com.zerotrust.votingapi.controller;

import com.zerotrust.votingapi.domain.DireccionIp;
import com.zerotrust.votingapi.domain.PruebaSeguridad;
import com.zerotrust.votingapi.domain.ResultadoPurga;
import com.zerotrust.votingapi.dto.request.PruebaSeguridadRequest;
import com.zerotrust.votingapi.dto.request.ReinicioSeguridadRequest;
import com.zerotrust.votingapi.dto.response.PruebaAceptadaResponse;
import com.zerotrust.votingapi.dto.response.PurgaRealizadaResponse;
import com.zerotrust.votingapi.dto.response.ReinicioAceptadoResponse;
import com.zerotrust.votingapi.exception.SolicitudSeguridadInvalidaException;
import com.zerotrust.votingapi.mapper.MapperPanelSeguridad;
import com.zerotrust.votingapi.service.ServicioSeguridadAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Funciones de seguridad del panel de administración (ver docs/contratos.md).
 *
 * Solo lo invoca el BFF autenticado con el token compartido. No usa CORS.
 * Los errores 400 y 503 los traduce GlobalExceptionHandler a {"error": "<mensaje para el panel>"}.
 */
@RestController
@RequestMapping("/internal/admin/security")
public final class AdminSeguridadController {

    static final String CABECERA_TOKEN = "X-Admin-Token";
    static final String FALTAN_DATOS_PRUEBA = "Indique la IP y el número de votos.";

    private final byte[] token;
    private final ServicioSeguridadAdmin servicio;

    public AdminSeguridadController(@Value("${ADMIN_API_TOKEN}") String clave, ServicioSeguridadAdmin servicio) {
        if (clave == null || clave.length() < 32) {
            throw new IllegalStateException("ADMIN_API_TOKEN requiere al menos 32 caracteres");
        }
        this.token = clave.getBytes(StandardCharsets.UTF_8);
        this.servicio = servicio;
    }

    @PostMapping("/drills")
    public ResponseEntity<?> simular(
            @RequestHeader(value = CABECERA_TOKEN, required = false) String recibido,
            @RequestBody(required = false) PruebaSeguridadRequest datos) {
        if (!autorizado(recibido)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (datos == null) {
            throw new SolicitudSeguridadInvalidaException(FALTAN_DATOS_PRUEBA);
        }
        PruebaSeguridad prueba = PruebaSeguridad.de(datos.ip(), datos.votes());
        servicio.simular(prueba);
        return ResponseEntity.status(HttpStatus.ACCEPTED).cacheControl(CacheControl.noStore())
                .body(new PruebaAceptadaResponse(true, prueba.ip().valor(), prueba.votos(), servicio.destinoComandos()));
    }

    @PostMapping("/reset")
    public ResponseEntity<?> reiniciar(
            @RequestHeader(value = CABECERA_TOKEN, required = false) String recibido,
            @RequestBody(required = false) ReinicioSeguridadRequest datos) {
        if (!autorizado(recibido)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        DireccionIp ip = new DireccionIp(datos == null ? null : datos.ip());
        servicio.reiniciar(ip);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new ReinicioAceptadoResponse(true, ip.valor()));
    }

    @PostMapping("/purge")
    public ResponseEntity<?> purgar(@RequestHeader(value = CABECERA_TOKEN, required = false) String recibido) {
        if (!autorizado(recibido)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        ResultadoPurga resultado = servicio.purgar();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new PurgaRealizadaResponse(true, resultado.incidentes(), resultado.ips()));
    }

    @GetMapping("/incidents")
    public ResponseEntity<?> incidentes(@RequestHeader(value = CABECERA_TOKEN, required = false) String recibido) {
        if (!autorizado(recibido)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(MapperPanelSeguridad.aResponse(servicio.panel()));
    }

    private boolean autorizado(String recibido) {
        return recibido != null && MessageDigest.isEqual(token, recibido.getBytes(StandardCharsets.UTF_8));
    }
}
