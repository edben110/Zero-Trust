package com.zerotrust.votingapi.adapter.web;

import com.zerotrust.votingapi.domain.Cedula;
import com.zerotrust.votingapi.exception.SolicitudVotoInvalidaException;
import com.zerotrust.votingapi.service.ServicioIdentificacion;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// POST y no GET: la cédula viaja en el cuerpo de la petición, nunca en la URL.
@RestController
public class ConsultaVoterIdController {

    private final ServicioIdentificacion servicio;

    public ConsultaVoterIdController(ServicioIdentificacion servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/voter-id")
    public ResponseEntity<VoterIdResponse> consultar(@RequestBody(required = false) ConsultaVoterIdRequest peticion) {
        Cedula cedula = convertir(peticion == null ? null : peticion.cedulaTexto());
        // no-store: ni el navegador ni un proxy deben guardar el UUID en caché.
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new VoterIdResponse(servicio.identificar(cedula).valor()));
    }

    private static Cedula convertir(String texto) {
        try {
            return new Cedula(texto);
        } catch (IllegalArgumentException e) {
            throw new SolicitudVotoInvalidaException(List.of("cedula"));
        }
    }
}
