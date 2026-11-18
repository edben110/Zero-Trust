-- =============================================================================
-- Observabilidad de seguridad: origen de la alerta + estado vigente por IP
-- Idempotente: zt-bootstrap lo ejecuta en cada convergencia de vm-brokers.
-- =============================================================================

BEGIN;

-- LIVE  = voto real consumido de raw_votes
-- DRILL = prueba controlada lanzada desde el panel de administración
ALTER TABLE fraud_incidents ADD COLUMN IF NOT EXISTS origin VARCHAR(10);
UPDATE fraud_incidents SET origin = 'LIVE' WHERE origin IS NULL;
ALTER TABLE fraud_incidents ALTER COLUMN origin SET DEFAULT 'LIVE';
ALTER TABLE fraud_incidents ALTER COLUMN origin SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_fraud_incidents_origin'
    ) THEN
        ALTER TABLE fraud_incidents
            ADD CONSTRAINT chk_fraud_incidents_origin CHECK (origin IN ('LIVE', 'DRILL'));
    END IF;
END
$$;

-- Consulta forense por IP ordenada por tiempo.
CREATE INDEX IF NOT EXISTS idx_fraud_incidents_ip_detected_at
    ON fraud_incidents (ip, detected_at DESC);

-- Proyección del patrón State: una fila por IP con su nivel vigente.
-- La escribe bot_detector_service en cada evaluación; BLOQUEADA solo sale de
-- ese estado con una intervención administrativa desde el panel.
CREATE TABLE IF NOT EXISTS ip_risk_state (
    ip             VARCHAR(45) PRIMARY KEY,
    risk_level     VARCHAR(20) NOT NULL CHECK (risk_level IN ('NORMAL', 'SOSPECHOSA', 'BLOQUEADA')),
    vote_count     INT         NOT NULL CHECK (vote_count >= 0),
    window_seconds INT         NOT NULL CHECK (window_seconds > 0),
    first_seen     TIMESTAMPTZ NOT NULL,
    last_seen      TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ip_risk_state_level ON ip_risk_state (risk_level);
CREATE INDEX IF NOT EXISTS idx_ip_risk_state_last_seen ON ip_risk_state (last_seen DESC);

COMMIT;
