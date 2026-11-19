package com.zerotrust.votingapi.service;

import com.zerotrust.votingapi.exception.CanalSeguridadNoDisponibleException;
import com.zerotrust.votingapi.domain.DireccionIp;
import com.zerotrust.votingapi.exception.OperacionSeguridadNoDisponibleException;
import com.zerotrust.votingapi.domain.PanelSeguridad;
import com.zerotrust.votingapi.domain.PruebaSeguridad;
import com.zerotrust.votingapi.domain.ResultadoPurga;
import com.zerotrust.votingapi.domain.UmbralesSeguridad;
import com.zerotrust.votingapi.port.ConsultaSeguridad;
import com.zerotrust.votingapi.port.PublicadorComandosSeguridad;

/**
 * Casos de uso del panel de seguridad: simulacros, reinicios, purga y consulta.
 *
 * Los simulacros no escriben en raw_votes: el escrutinio y la auditoría electoral quedan intactos.
 */
public class ServicioSeguridadAdmin {

    public static final int MAX_ESTADOS = 100;
    public static final int MAX_INCIDENTES = 200;

    static final String PRUEBA_NO_PUBLICADA = "No se pudo publicar la prueba en Kafka.";
    static final String REINICIO_NO_PUBLICADO = "No se pudo publicar el reinicio en Kafka.";
    static final String DETECTOR_NO_REINICIADO = "No se pudo reiniciar el detector en Kafka; no se borraron registros.";
    static final String REGISTROS_NO_PURGADOS = "El detector se reinició, pero no se pudo purgar PostgreSQL.";
    static final String CONSULTA_NO_DISPONIBLE = "No se pudo consultar PostgreSQL.";

    private final ConsultaSeguridad consulta;
    private final PublicadorComandosSeguridad publicador;
    private final UmbralesSeguridad umbrales;

    public ServicioSeguridadAdmin(ConsultaSeguridad consulta, PublicadorComandosSeguridad publicador,
                                  UmbralesSeguridad umbrales) {
        this.consulta = consulta;
        this.publicador = publicador;
        this.umbrales = umbrales;
    }

    public void simular(PruebaSeguridad prueba) {
        try {
            for (int i = 0; i < prueba.votos(); i++) {
                publicador.evaluar(prueba.ip());
            }
        } catch (CanalSeguridadNoDisponibleException e) {
            throw new OperacionSeguridadNoDisponibleException(PRUEBA_NO_PUBLICADA, e);
        }
    }

    public void reiniciar(DireccionIp ip) {
        try {
            publicador.reiniciar(ip);
        } catch (CanalSeguridadNoDisponibleException e) {
            throw new OperacionSeguridadNoDisponibleException(REINICIO_NO_PUBLICADO, e);
        }
    }

    /**
     * Primero ordena al detector olvidar sus contextos en memoria; si Kafka no confirma no se borra nada,
     * para no dejar el panel en ceros mientras el detector sigue contando.
     * No toca el padrón, los votos aceptados ni los intentos de región.
     */
    public ResultadoPurga purgar() {
        try {
            publicador.reiniciarTodo();
        } catch (CanalSeguridadNoDisponibleException e) {
            throw new OperacionSeguridadNoDisponibleException(DETECTOR_NO_REINICIADO, e);
        }
        try {
            return consulta.purgar();
        } catch (CanalSeguridadNoDisponibleException e) {
            throw new OperacionSeguridadNoDisponibleException(REGISTROS_NO_PURGADOS, e);
        }
    }

    public PanelSeguridad panel() {
        try {
            return new PanelSeguridad(umbrales, consulta.resumen(),
                    consulta.estados(MAX_ESTADOS), consulta.incidentesRecientes(MAX_INCIDENTES));
        } catch (CanalSeguridadNoDisponibleException e) {
            throw new OperacionSeguridadNoDisponibleException(CONSULTA_NO_DISPONIBLE, e);
        }
    }

    public String destinoComandos() {
        return publicador.destino();
    }
}
