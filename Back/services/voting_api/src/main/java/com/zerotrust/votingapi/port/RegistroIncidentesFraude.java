package com.zerotrust.votingapi.port;

import com.zerotrust.votingapi.domain.FraudKind;
import com.zerotrust.votingapi.domain.SourceIp;

/**
 * Puerto de salida para reportar incidentes de fraude electoral.
 *
 * La implementación publica el evento en Kafka y bot_detector_service lo
 * persiste en fraud_incidents, igual que hace con las alertas del patrón
 * State. El reporte es best-effort: un fallo al notificar nunca debe cambiar
 * la respuesta que recibe el votante.
 */
public interface RegistroIncidentesFraude {

    void registrar(FraudKind tipo, SourceIp ip);
}
