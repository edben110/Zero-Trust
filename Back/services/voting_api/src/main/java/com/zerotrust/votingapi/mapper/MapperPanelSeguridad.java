package com.zerotrust.votingapi.mapper;

import com.zerotrust.votingapi.domain.EstadoIp;
import com.zerotrust.votingapi.domain.IncidenteSeguridad;
import com.zerotrust.votingapi.domain.PanelSeguridad;
import com.zerotrust.votingapi.domain.ResumenSeguridad;
import com.zerotrust.votingapi.domain.UmbralesSeguridad;
import com.zerotrust.votingapi.dto.response.PanelSeguridadResponse;
import com.zerotrust.votingapi.dto.response.PanelSeguridadResponse.Estado;
import com.zerotrust.votingapi.dto.response.PanelSeguridadResponse.Incidente;
import com.zerotrust.votingapi.dto.response.PanelSeguridadResponse.Resumen;
import com.zerotrust.votingapi.dto.response.PanelSeguridadResponse.Umbrales;

import java.time.Instant;

/**
 * Convierte el panel de seguridad del dominio al cuerpo JSON que espera el BFF.
 */
public final class MapperPanelSeguridad {

    private MapperPanelSeguridad() {
    }

    public static PanelSeguridadResponse aResponse(PanelSeguridad panel) {
        return new PanelSeguridadResponse(
                umbrales(panel.umbrales()),
                resumen(panel.resumen()),
                panel.estados().stream().map(MapperPanelSeguridad::estado).toList(),
                panel.incidentes().stream().map(MapperPanelSeguridad::incidente).toList());
    }

    private static Umbrales umbrales(UmbralesSeguridad u) {
        return new Umbrales(u.sospechosa(), u.bloqueada(), u.ventanaSegundos());
    }

    private static Resumen resumen(ResumenSeguridad r) {
        return new Resumen(r.incidentes(), r.reales(), r.simulacros(), r.duplicados(),
                r.regionIncorrecta(), r.ipsConIncidentes(), r.riesgoActual());
    }

    private static Estado estado(EstadoIp e) {
        return new Estado(e.ip(), e.nivelRiesgo(), e.votos(), e.ventanaSegundos(),
                iso(e.primeraVez()), iso(e.ultimaVez()));
    }

    private static Incidente incidente(IncidenteSeguridad i) {
        return new Incidente(i.id(), i.ip(), i.tipo(), i.nivelRiesgo(), i.votos(),
                i.ventanaSegundos(), i.origen(), iso(i.detectadoEn()));
    }

    private static String iso(Instant instante) {
        return instante == null ? null : instante.toString();
    }
}
