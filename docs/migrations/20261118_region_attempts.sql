-- Ejecutar en PostgreSQL existente ANTES de arrancar la versión nueva de voting_api.
-- zt-bootstrap.sh la aplica en cada convergencia de vm-brokers: debe ser idempotente.
-- El borrado de full_name es irreversible: hacer respaldo si se requieren esos datos.
BEGIN;
CREATE TABLE IF NOT EXISTS voter_region_attempts (
    run_id UUID NOT NULL,
    voter_uuid VARCHAR(36) NOT NULL,
    failures SMALLINT NOT NULL CHECK (failures BETWEEN 1 AND 2),
    PRIMARY KEY (run_id, voter_uuid)
);
ALTER TABLE registered_voters DROP COLUMN IF EXISTS full_name;
COMMIT;
