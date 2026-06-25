-- ============================================================
-- V1__create_users_table.sql
-- auth-service — schéma initial
-- ============================================================

CREATE TABLE users (
    id          UUID            NOT NULL DEFAULT gen_random_uuid(),
    email       VARCHAR(255)    NOT NULL,
    password    VARCHAR(255)    NOT NULL,
    first_name  VARCHAR(100)    NOT NULL,
    last_name   VARCHAR(100)    NOT NULL,
    role        VARCHAR(20)     NOT NULL DEFAULT 'USER',
    status      VARCHAR(30)     NOT NULL DEFAULT 'PENDING_VERIFICATION',
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT chk_users_status CHECK (status IN (
        'PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'DELETED'
    ))
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_status ON users (status);

COMMENT ON TABLE users IS 'Utilisateurs du système FinTrack — géré par auth-service';
COMMENT ON COLUMN users.id IS 'Identifiant unique UUID v4';
COMMENT ON COLUMN users.email IS 'Email normalisé en lowercase, unique';
COMMENT ON COLUMN users.status IS 'Cycle de vie du compte utilisateur';
