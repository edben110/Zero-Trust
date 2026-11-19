package com.zerotrust.votingapi.adapter.web;

import com.zerotrust.votingapi.domain.EstadoIp;
import com.zerotrust.votingapi.domain.IncidenteSeguridad;
import com.zerotrust.votingapi.domain.PanelSeguridad;
import com.zerotrust.votingapi.domain.ResumenSeguridad;
import com.zerotrust.votingapi.domain.UmbralesSeguridad;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Cuerpo de GET /internal/admin/security/incidents con los nombres que espera el panel.
 */
public record PanelSeguridadResponse(Umbrales thresholds, Resumen summary, List<Estado> ips, List<Incidente> incidents) {

    public record Umbrales(int suspicious, int blocked, int windowSeconds) {
        static Umbrales de(UmbralesSeguridad u) {
            return new Umbrales(u.sospechosa(), u.bloqueada(), u.ventanaSegundos());
        }
    }

    public record Resumen(long incidents, long live, long drills, long duplicates, long wrongRegion,
                          long ipsWithIncidents, Map<String, Long> currentRisk) {
        static Resumen de(ResumenSeguridad r) {
            return new Resumen(r.incidentes(), r.reales(), r.simulacros(), r.duplicados(),
                    r.regionIncorrecta(), r.ipsConIncidentes(), r.riesgoActual());
        }
    }

    public record Estado(String ip, String riskLevel, int voteCount, int windowSeconds,
                         String firstSeen, String lastSeen) {
        static Estado de(EstadoIp e) {
            return new Estado(e.ip(), e.nivelRiesgo(), e.votos(), e.ventanaSegundos(),
                    iso(e.primeraVez()), iso(e.ultimaVez()));
        }
    }

    public record Incidente(long id, String ip, String kind, String riskLevel, int voteCount,
                            int windowSeconds, String origin, String detectedAt) {
        static Incidente de(IncidenteSeguridad i) {
            return new Incidente(i.id(), i.ip(), i.tipo(), i.nivelRiesgo(), i.votos(),
                    i.ventanaSegundos(), i.origen(), iso(i.detectadoEn()));
        }
    }

    static PanelSeguridadResponse de(PanelSeguridad panel) {
        return new PanelSeguridadResponse(
                Umbrales.de(panel.umbrales()),
                Resumen.de(panel.resumen()),
                panel.estados().stream().map(Estado::de).toList(),
                panel.incidentes().stream().map(Incidente::de).toList());
    }

    private static String iso(Instant instante) {
        return instante == null ? null : instante.toString();
    }
}
