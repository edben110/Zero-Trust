package com.zerotrust.votingapi.port;

import com.zerotrust.votingapi.domain.DireccionIp;

/**
 * Comandos para bot_detector_service. Cada envío espera la confirmación del broker
 * y lanza CanalSeguridadNoDisponibleException si no llega.
 */
public interface PublicadorComandosSeguridad {

    void evaluar(DireccionIp ip);

    void reiniciar(DireccionIp ip);

    void reiniciarTodo();

    String destino();
}
