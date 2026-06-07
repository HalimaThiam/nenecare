-- =============================================================================
-- NeneCare – Schéma de la table audit_log
-- Sprint Alpha – feature/audit
-- Responsable : Hadja Mariama DIALLO
-- =============================================================================
--
-- Table entièrement isolée : aucune clé étrangère vers les tables métier.
-- Cela garantit que le journal subsiste même si une entité métier est supprimée,
-- et empêche toute cascade accidentelle.
--
-- Les permissions PostgreSQL sont restreintes :
--   - L'application (rôle nenecare_app)  → INSERT + SELECT uniquement
--   - L'administrateur DBA               → SELECT uniquement (audit externe)
--   - Personne                           → UPDATE / DELETE interdits
-- =============================================================================

CREATE TABLE IF NOT EXISTS audit_log (
    -- -------------------------------------------------------------------------
    -- Identifiant
    -- -------------------------------------------------------------------------
    id              BIGSERIAL       PRIMARY KEY,

    -- -------------------------------------------------------------------------
    -- Acteur
    -- -------------------------------------------------------------------------
    actor_id        VARCHAR(64)     NOT NULL,
    actor_role      VARCHAR(32)     NOT NULL,

    -- -------------------------------------------------------------------------
    -- Action
    -- -------------------------------------------------------------------------
    action          VARCHAR(64)     NOT NULL
                        CHECK (action IN (
                            'LOGIN_SUCCESS', 'LOGIN_FAILURE', 'LOGOUT',
                            'DOSSIER_CREATE', 'DOSSIER_READ',
                            'DOSSIER_UPDATE', 'DOSSIER_DELETE',
                            'PATIENTE_CREATE', 'PATIENTE_READ',
                            'PATIENTE_UPDATE', 'PATIENTE_DELETE',
                            'USER_CREATE', 'USER_REVOKE',
                            'KEY_EXCHANGE', 'ACCESS_DENIED'
                        )),

    -- -------------------------------------------------------------------------
    -- Ressource cible
    -- -------------------------------------------------------------------------
    resource_type   VARCHAR(64),    -- PATIENTE, DOSSIER_MEDICAL, DOSSIER_NEONATAL…
    resource_id     VARCHAR(64),    -- PK de la ressource cible

    -- -------------------------------------------------------------------------
    -- Contexte
    -- -------------------------------------------------------------------------
    ip_address      VARCHAR(45),    -- IPv4 ou IPv6
    details         TEXT,           -- JSON ou texte libre, SANS données sensibles

    -- -------------------------------------------------------------------------
    -- Horodatage UTC
    -- -------------------------------------------------------------------------
    timestamp       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- -------------------------------------------------------------------------
    -- Intégrité – HMAC-SHA256 (Base64, 44 caractères)
    -- -------------------------------------------------------------------------
    hmac_signature  CHAR(44)        NOT NULL
);

-- =============================================================================
-- Index
-- =============================================================================
CREATE INDEX IF NOT EXISTS idx_audit_actor
    ON audit_log (actor_id);

CREATE INDEX IF NOT EXISTS idx_audit_action
    ON audit_log (action);

CREATE INDEX IF NOT EXISTS idx_audit_resource
    ON audit_log (resource_type, resource_id);

CREATE INDEX IF NOT EXISTS idx_audit_timestamp
    ON audit_log (timestamp DESC);

-- Index composite pour les requêtes de sécurité (ex : brute-force detection)
CREATE INDEX IF NOT EXISTS idx_audit_actor_action_ts
    ON audit_log (actor_id, action, timestamp DESC);

-- =============================================================================
-- Sécurité : restriction des permissions sur la table
-- =============================================================================
-- Révoquer tous les droits par défaut
REVOKE ALL ON audit_log FROM PUBLIC;

-- L'application ne peut qu'insérer et lire
GRANT INSERT, SELECT ON audit_log TO nenecare_app;
GRANT USAGE, SELECT ON SEQUENCE audit_log_id_seq TO nenecare_app;

-- L'administrateur d'audit ne peut que lire
-- GRANT SELECT ON audit_log TO nenecare_audit_reader;

-- =============================================================================
-- Commentaires de documentation
-- =============================================================================
COMMENT ON TABLE  audit_log              IS 'Journal d''audit immuable – NeneCare DevSecOps M1-SSI';
COMMENT ON COLUMN audit_log.actor_id     IS 'Identifiant de l''utilisateur ayant effectué l''action';
COMMENT ON COLUMN audit_log.actor_role   IS 'Rôle au moment de l''action : MEDECIN | SAGE_FEMME | ADMIN';
COMMENT ON COLUMN audit_log.action       IS 'Code d''action normalisé (liste fermée via CHECK)';
COMMENT ON COLUMN audit_log.resource_type IS 'Type de la ressource cible';
COMMENT ON COLUMN audit_log.resource_id  IS 'Identifiant de la ressource cible (nullable pour LOGIN)';
COMMENT ON COLUMN audit_log.ip_address   IS 'Adresse IP de l''acteur (IPv4 ou IPv6)';
COMMENT ON COLUMN audit_log.details      IS 'Détails libres (JSON) – ne jamais stocker de données médicales';
COMMENT ON COLUMN audit_log.timestamp    IS 'Horodatage UTC de l''événement';
COMMENT ON COLUMN audit_log.hmac_signature IS 'HMAC-SHA256 Base64 sur tous les champs – garantit l''intégrité';
