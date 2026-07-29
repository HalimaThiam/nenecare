-- =============================================================================
-- NeneCare - Schema de reference de la table audit_log
-- Sprint Alpha - feature/audit
-- Responsable : Hadja Mariama DIALLO
-- =============================================================================
--
-- ATTENTION - ETAT ACTUEL :
-- Flyway n'est pas encore branche sur le projet. Aujourd'hui c'est Hibernate
-- qui cree la table (spring.jpa.hibernate.ddl-auto=update) a partir de
-- l'entite sn.esp.nenecare.audit.model.AuditLog.
--
-- Ce fichier a donc deux usages :
--   1. documenter le schema cible et les contraintes de securite attendues ;
--   2. servir de migration prete a l'emploi le jour ou l'equipe ajoute Flyway
--      (ajouter flyway-core au pom.xml et passer ddl-auto a "validate").
--
-- Les colonnes ci-dessous correspondent EXACTEMENT a l'entite JPA actuelle.
-- Toute evolution de l'entite doit etre repercutee ici.
--
-- Table entierement isolee : aucune cle etrangere vers les tables metier.
-- Le journal subsiste donc meme si une entite metier est supprimee, et aucune
-- cascade ne peut l'effacer accidentellement.
-- =============================================================================

CREATE TABLE IF NOT EXISTS audit_log (
    -- Identifiant
    id              BIGSERIAL       PRIMARY KEY,

    -- Horodatage de l'evenement
    timestamp       TIMESTAMP       NOT NULL,

    -- Qui a agi
    utilisateur     VARCHAR(100)    NOT NULL,
    role            VARCHAR(50)     NOT NULL,

    -- Quelle action
    -- Exemples : LOGIN_SUCCESS, LOGIN_FAILURE, LOGIN_BLOCKED, LOGOUT,
    --            DOSSIER_CREATE, DOSSIER_READ, DOSSIER_UPDATE, DOSSIER_DELETE,
    --            USER_CREATE, USER_REVOKE, ACCESS_DENIED
    action          VARCHAR(100)    NOT NULL,

    -- Sur quelle ressource (nullable : une connexion ne cible rien)
    ressource       VARCHAR(200),

    -- Contexte
    details         TEXT,           -- texte libre, SANS donnees medicales
    adresse_ip      VARCHAR(45),    -- IPv4 ou IPv6
    succes          BOOLEAN         NOT NULL,

    -- Integrite - HMAC-SHA256 encode en Base64
    signature_hmac  VARCHAR(500)    NOT NULL
);

-- =============================================================================
-- Index
-- =============================================================================
CREATE INDEX IF NOT EXISTS idx_audit_utilisateur ON audit_log (utilisateur);
CREATE INDEX IF NOT EXISTS idx_audit_action      ON audit_log (action);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp   ON audit_log (timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_succes      ON audit_log (succes);

-- Index composite pour la detection de force brute (US-04)
CREATE INDEX IF NOT EXISTS idx_audit_util_action_ts
    ON audit_log (utilisateur, action, timestamp DESC);

-- =============================================================================
-- Securite : restriction des permissions sur la table
-- =============================================================================
-- A executer manuellement par le DBA apres creation du role applicatif.
-- Ces instructions sont commentees car elles echouent tant que les roles
-- nenecare_app / nenecare_audit_reader n'existent pas sur l'instance :
--
--   REVOKE ALL ON audit_log FROM PUBLIC;
--   GRANT INSERT, SELECT ON audit_log TO nenecare_app;
--   GRANT USAGE, SELECT ON SEQUENCE audit_log_id_seq TO nenecare_app;
--   -- Auditeur externe, lecture seule :
--   -- GRANT SELECT ON audit_log TO nenecare_audit_reader;
--
-- L'application n'a alors ni UPDATE ni DELETE : meme un compte applicatif
-- compromis ne peut pas effacer ses propres traces.

-- =============================================================================
-- Documentation
-- =============================================================================
COMMENT ON TABLE  audit_log                IS 'Journal d''audit - NeneCare DevSecOps M1-SSI';
COMMENT ON COLUMN audit_log.utilisateur    IS 'Identifiant de l''utilisateur ayant effectue l''action';
COMMENT ON COLUMN audit_log.role           IS 'Role au moment de l''action (RBAC)';
COMMENT ON COLUMN audit_log.action         IS 'Code d''action normalise';
COMMENT ON COLUMN audit_log.ressource      IS 'Ressource ciblee (nullable pour les connexions)';
COMMENT ON COLUMN audit_log.details        IS 'Details libres - ne jamais stocker de donnees medicales';
COMMENT ON COLUMN audit_log.adresse_ip     IS 'Adresse IP de l''acteur (IPv4 ou IPv6)';
COMMENT ON COLUMN audit_log.succes         IS 'Issue de l''action : true = reussie, false = refusee/echouee';
COMMENT ON COLUMN audit_log.signature_hmac IS 'HMAC-SHA256 Base64 des champs signes - garantit l''integrite';
