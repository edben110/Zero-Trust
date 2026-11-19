package com.zerotrust.votingapi.domain;

import java.util.List;

/**
 * Vista completa del panel de seguridad.
 */
public record PanelSeguridad(UmbralesSeguridad umbrales, ResumenSeguridad resumen,
                             List<EstadoIp> estados, List<IncidenteSeguridad> incidentes) {

    public PanelSeguridad {
        if (umbrales == null || resumen == null || estados == null || incidentes == null) {
            throw new IllegalArgumentException("El panel de seguridad requiere todos sus datos");
        }
        estados = List.copyOf(estados);
        incidentes = List.copyOf(incidentes);
    }
}
