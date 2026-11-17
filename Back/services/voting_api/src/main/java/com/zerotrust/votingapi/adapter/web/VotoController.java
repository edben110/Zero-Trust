package com.zerotrust.votingapi.adapter.web;

import com.zerotrust.votingapi.adapter.config.ResolutorIpOrigen;
import com.zerotrust.votingapi.domain.Catalogo;
import com.zerotrust.votingapi.domain.SolicitudVoto;
import com.zerotrust.votingapi.domain.SolicitudVotoInvalidaException;
import com.zerotrust.votingapi.domain.SourceIp;
import com.zerotrust.votingapi.service.ServicioVotacion;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class VotoController {

    private final ServicioVotacion servicio;
    private final Catalogo catalogo;
    private final ResolutorIpOrigen resolutorIp;

    public VotoController(ServicioVotacion servicio, Catalogo catalogo, ResolutorIpOrigen resolutorIp) {
        this.servicio = servicio;
        this.catalogo = catalogo;
        this.resolutorIp = resolutorIp;
    }

    @PostMapping("/vote")
    public ComprobanteResponse votar(@RequestBody(required = false) VotoRequest peticion, HttpServletRequest http) {
        if (peticion == null) {
            throw new SolicitudVotoInvalidaException(List.of("voterId", "candidateId", "region"));
        }
        SolicitudVoto solicitud = SolicitudVoto.desde(peticion.voterId(), peticion.candidateId(), peticion.region(),
                catalogo);
        SourceIp ip = resolutorIp.resolver(http.getRemoteAddr(), http.getHeader("X-Forwarded-For"));
        return ComprobanteResponse.de(servicio.votar(solicitud, ip));
    }
}
