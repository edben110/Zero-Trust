package com.zerotrust.votingapi.controller;

import com.zerotrust.votingapi.config.ResolutorIpOrigen;
import com.zerotrust.votingapi.config.RepositorioCatalogo;
import com.zerotrust.votingapi.domain.SolicitudVoto;
import com.zerotrust.votingapi.dto.request.VotoRequest;
import com.zerotrust.votingapi.dto.response.ComprobanteResponse;
import com.zerotrust.votingapi.exception.SolicitudVotoInvalidaException;
import com.zerotrust.votingapi.domain.SourceIp;
import com.zerotrust.votingapi.exception.ServicioNoDisponibleException;
import com.zerotrust.votingapi.mapper.MapperComprobante;
import com.zerotrust.votingapi.service.ServicioVotacion;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class VotoController {

    private final ServicioVotacion servicio;
    private final RepositorioCatalogo catalogo;
    private final ResolutorIpOrigen resolutorIp;

    public VotoController(ServicioVotacion servicio, RepositorioCatalogo catalogo, ResolutorIpOrigen resolutorIp) {
        this.servicio = servicio;
        this.catalogo = catalogo;
        this.resolutorIp = resolutorIp;
    }

    @PostMapping("/vote")
    public ComprobanteResponse votar(@RequestBody(required = false) VotoRequest peticion, HttpServletRequest http) {
        if (catalogo.actual().candidatos().size() < 2) {
            throw new ServicioNoDisponibleException("Se requieren al menos dos candidatos") { };
        }
        if (peticion == null) {
            throw new SolicitudVotoInvalidaException(List.of("voterId", "candidateId", "region"));
        }
        SolicitudVoto solicitud = SolicitudVoto.desde(peticion.voterId(), peticion.candidateId(), peticion.region(),
                catalogo.actual());
        SourceIp ip = resolutorIp.resolver(http.getRemoteAddr(), http.getHeader("X-Forwarded-For"));
        return MapperComprobante.aResponse(servicio.votar(solicitud, ip));
    }
}
