-- =============================================================================
-- Zero Trust Voting Platform - Schema
-- Base de datos: zerotrust
-- Este archivo va montado en /docker-entrypoint-initdb.d/ del contenedor
-- postgres-db en vm-brokers para que se ejecute automaticamente al arrancar.
-- =============================================================================

-- Votantes registrados
-- Poblada por voter_registration_service (Sebas) al arrancar el sistema.
CREATE TABLE IF NOT EXISTS registered_voters (
    id            BIGSERIAL    PRIMARY KEY,
    voter_id      VARCHAR(36)  NOT NULL UNIQUE,  -- UUID
    full_name     VARCHAR(255) NOT NULL,
    region        VARCHAR(100) NOT NULL,
    registered_at TIMESTAMPTZ  NOT NULL
);

-- Auditoria de votos aceptados
-- Cada voto que pasa validacion queda registrado aqui de forma inmutable.
-- Poblada por analytics_archiver (Sebas).
CREATE TABLE IF NOT EXISTS vote_audit (
    id           BIGSERIAL    PRIMARY KEY,
    vote_id      VARCHAR(36)  NOT NULL UNIQUE,  -- UUID
    voter_id     VARCHAR(36)  NOT NULL,
    candidate_id VARCHAR(36)  NOT NULL,
    region       VARCHAR(100) NOT NULL,
    source_ip    VARCHAR(45)  NOT NULL,
    voted_at     TIMESTAMPTZ  NOT NULL
);

-- Incidentes de fraude detectados por bot_detector_service
-- Append-only: nunca se actualiza un registro, solo se insertan nuevos.
-- Poblada por bot_detector_service (Daniel).
CREATE TABLE IF NOT EXISTS fraud_incidents (
    id             BIGSERIAL    PRIMARY KEY,
    ip             VARCHAR(45)  NOT NULL,
    risk_level     VARCHAR(20)  NOT NULL,  -- NORMAL | SOSPECHOSA | BLOQUEADA
    vote_count     INT          NOT NULL,
    window_seconds INT          NOT NULL,
    detected_at    TIMESTAMPTZ  NOT NULL
);

-- Indices para consultas forenses frecuentes
CREATE INDEX IF NOT EXISTS idx_vote_audit_voter_id    ON vote_audit (voter_id);
CREATE INDEX IF NOT EXISTS idx_vote_audit_voted_at    ON vote_audit (voted_at);
CREATE INDEX IF NOT EXISTS idx_fraud_incidents_ip     ON fraud_incidents (ip);
CREATE INDEX IF NOT EXISTS idx_fraud_incidents_level  ON fraud_incidents (risk_level);
