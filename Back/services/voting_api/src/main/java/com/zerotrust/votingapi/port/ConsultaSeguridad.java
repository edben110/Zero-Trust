package com.zerotrust.votingapi.port;

import com.zerotrust.votingapi.domain.EstadoIp;
import com.zerotrust.votingapi.domain.IncidenteSeguridad;
import com.zerotrust.votingapi.domain.ResultadoPurga;
import com.zerotrust.votingapi.domain.ResumenSeguridad;

import java.util.List;

// Lanza CanalSeguridadNoDisponibleException si la base de datos no responde.
public interface ConsultaSeguridad {

    ResumenSeguridad resumen();

    List<EstadoIp> estados(int limite);

    List<IncidenteSeguridad> incidentesRecientes(int limite);

    ResultadoPurga purgar();
}
