-- =============================================================================
-- Zero Trust Voting Platform - Schema
-- Base de datos: zerotrust
-- Este archivo va montado en /docker-entrypoint-initdb.d/ del contenedor
-- postgres-db en vm-brokers para que se ejecute automaticamente al arrancar.
-- =============================================================================

-- Votantes registrados
-- Poblada por voter_registration_service (Sebas) al arrancar el sistema.
-- voter_id es el UUID derivado de la cedula (el mismo que devuelve POST /voter-id); la cedula no se guarda.
CREATE TABLE IF NOT EXISTS registered_voters (
    id            BIGSERIAL    PRIMARY KEY,
    voter_id      VARCHAR(36)  NOT NULL UNIQUE,  -- UUID
    full_name     VARCHAR(255) NOT NULL,
    region        VARCHAR(100) NOT NULL,
    registered_at TIMESTAMPTZ  NOT NULL
);

-- Voto unico por eleccion
-- voting_api registra aqui el voterId (UUID derivado de la cedula con HMAC-SHA-256) en la misma transaccion
-- que publica el voto. La cedula NUNCA se guarda: solo el UUID.
-- La restriccion UNIQUE es la que impide el voto doble, incluso con solicitudes simultaneas.
CREATE TABLE IF NOT EXISTS voter_election_usage (
    election_id VARCHAR(64) NOT NULL,
    voter_uuid  VARCHAR(36) NOT NULL,
    used_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_voter_election_usage UNIQUE (election_id, voter_uuid)
);

-- Epoch de la votación visible. Rotarlo permite otra jornada con el mismo
-- padrón y candidatos sin mezclar los resultados anteriores.
CREATE TABLE IF NOT EXISTS election_state (
    id     INT PRIMARY KEY CHECK (id = 1),
    run_id UUID NOT NULL
);
INSERT INTO election_state (id, run_id) VALUES (1, gen_random_uuid())
ON CONFLICT (id) DO NOTHING;

-- Proyección transaccional de votos aceptados, fuente común de ambos escrutinios.
-- Las filas de jornadas anteriores quedan separadas por run_id.
CREATE TABLE IF NOT EXISTS accepted_votes (
    run_id       UUID         NOT NULL,
    vote_id      VARCHAR(36)  NOT NULL UNIQUE,
    voter_uuid   VARCHAR(36)  NOT NULL,
    candidate_id VARCHAR(36)  NOT NULL,
    region       VARCHAR(100) NOT NULL,
    voted_at     TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (run_id, voter_uuid)
);
CREATE INDEX IF NOT EXISTS idx_accepted_votes_run_region ON accepted_votes (run_id, region);

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
