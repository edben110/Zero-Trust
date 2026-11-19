-- =============================================================================
-- Incidentes de fraude electoral además del patrón por IP
-- Idempotente: zt-bootstrap lo ejecuta en cada convergencia de vm-brokers.
--
-- kind = IP_PATTERN     → lo detecta bot_detector_service con su patrón State
--        DUPLICATE_VOTE → voting_api rechazó un segundo voto del mismo documento
--        WRONG_REGION   → voting_api anuló el voto por dos regiones incorrectas
-- =============================================================================

BEGIN;

ALTER TABLE fraud_incidents ADD COLUMN IF NOT EXISTS kind VARCHAR(20);
UPDATE fraud_incidents SET kind = 'IP_PATTERN' WHERE kind IS NULL;
ALTER TABLE fraud_incidents ALTER COLUMN kind SET DEFAULT 'IP_PATTERN';
ALTER TABLE fraud_incidents ALTER COLUMN kind SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_fraud_incidents_kind'
    ) THEN
        ALTER TABLE fraud_incidents
            ADD CONSTRAINT chk_fraud_incidents_kind
            CHECK (kind IN ('IP_PATTERN', 'DUPLICATE_VOTE', 'WRONG_REGION'));
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_fraud_incidents_kind ON fraud_incidents (kind);

COMMIT;
