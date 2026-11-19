package com.zerotrust.botdetector.port;

import java.time.Instant;

// Una fila por IP para que el panel muestre el estado sin consultar la memoria del detector.
public interface RiskStateRepository {

    void actualizar(String ip, String riskLevel, int voteCount, int windowSeconds, Instant seenAt);
}
