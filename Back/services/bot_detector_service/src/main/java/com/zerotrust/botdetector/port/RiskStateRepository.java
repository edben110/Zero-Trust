package com.zerotrust.botdetector.port;

import java.time.Instant;

/**
 * Puerto de salida: proyecta el estado vigente del patrón State en PostgreSQL.
 *
 * Es una vista operativa (una fila por IP) para que el panel de administración
 * pueda mostrar NORMAL/SOSPECHOSA/BLOQUEADA sin consultar la memoria del
 * proceso detector.
 */
public interface RiskStateRepository {

    void actualizar(String ip, String riskLevel, int voteCount, int windowSeconds, Instant seenAt);
}
