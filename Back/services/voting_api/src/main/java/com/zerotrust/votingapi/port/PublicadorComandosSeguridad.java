package com.zerotrust.votingapi.port;

import com.zerotrust.votingapi.domain.DireccionIp;

// Cada envío espera la confirmación del broker; si no llega, lanza CanalSeguridadNoDisponibleException.
public interface PublicadorComandosSeguridad {

    void evaluar(DireccionIp ip);

    void reiniciar(DireccionIp ip);

    void reiniciarTodo();

    String destino();
}
