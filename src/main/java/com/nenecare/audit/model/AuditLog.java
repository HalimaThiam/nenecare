package com.nenecare.audit.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entité AuditLog – Journal d'audit isolé de NeneCare
 *
 * Table dédiée, sans FK vers les tables métier, pour garantir
 * l'intégrité forensique du journal même en cas d'altération des données.
 *
 * Chaque entrée est signée par un HMAC-SHA256 calculé sur :
 *   actorId | action | resourceType | resourceId | timestamp | details
 *
 * Sprint Alpha – feature/audit
 * Responsable : Hadja Mariama DIALLO
 */
@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_actor",     columnList = "actor_id"),
        @Index(name = "idx_audit_action",    columnList = "action"),
        @Index(name = "idx_audit_resource",  columnList = "resource_type, resource_id"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
public class AuditLog {

    // -------------------------------------------------------------------------
    // Identifiant
    // -------------------------------------------------------------------------

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    // -------------------------------------------------------------------------
    // Qui a effectué l'action ?
    // -------------------------------------------------------------------------

    /**
     * Identifiant de l'utilisateur (rôle : MEDECIN, SAGE_FEMME, ADMIN).
     * Stocké en clair : utile pour les investigations même si le compte est supprimé.
     */
    @Column(name = "actor_id", nullable = false, length = 64)
    private String actorId;

    /** Rôle de l'acteur au moment de l'action. */
    @Column(name = "actor_role", nullable = false, length = 32)
    private String actorRole;

    // -------------------------------------------------------------------------
    // Quelle action ?
    // -------------------------------------------------------------------------

    /**
     * Code d'action normalisé.
     * Exemples : LOGIN_SUCCESS, LOGIN_FAILURE, DOSSIER_CREATE,
     *            DOSSIER_READ, DOSSIER_UPDATE, DOSSIER_DELETE,
     *            USER_CREATE, USER_REVOKE, KEY_EXCHANGE
     */
    @Column(name = "action", nullable = false, length = 64)
    private String action;

    // -------------------------------------------------------------------------
    // Sur quelle ressource ?
    // -------------------------------------------------------------------------

    /**
     * Type de ressource ciblée.
     * Exemples : PATIENTE, DOSSIER_MEDICAL, DOSSIER_NEONATAL, USER, SESSION
     */
    @Column(name = "resource_type", length = 64)
    private String resourceType;

    /** Identifiant de la ressource ciblée (peut être null pour LOGIN). */
    @Column(name = "resource_id", length = 64)
    private String resourceId;

    // -------------------------------------------------------------------------
    // Contexte
    // -------------------------------------------------------------------------

    /** Adresse IP de l'acteur au moment de l'action. */
    @Column(name = "ip_address", length = 45)   // IPv6 max = 39 chars
    private String ipAddress;

    /**
     * Détails libres (JSON ou texte).
     * Ex : {"champs_modifies": ["tension", "poids"]}, "Tentative #3"
     * Ne jamais stocker de données médicales sensibles ici.
     */
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    // -------------------------------------------------------------------------
    // Horodatage
    // -------------------------------------------------------------------------

    /**
     * Timestamp UTC de l'événement, fixé à la création.
     * Utiliser Instant (UTC) plutôt que LocalDateTime pour éviter
     * les ambiguïtés de fuseau horaire.
     */
    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    // -------------------------------------------------------------------------
    // Intégrité – HMAC-SHA256
    // -------------------------------------------------------------------------

    /**
     * Signature HMAC-SHA256 encodée en Base64 calculée sur la concaténation :
     *   actorId + "|" + actorRole + "|" + action + "|" +
     *   resourceType + "|" + resourceId + "|" +
     *   timestamp.toString() + "|" + details
     *
     * Calculée par AuditService avant la persistance.
     * Non modifiable après insertion (updatable = false).
     */
    @Column(name = "hmac_signature", nullable = false, updatable = false, length = 64)
    private String hmacSignature;

    // -------------------------------------------------------------------------
    // Lifecycle JPA
    // -------------------------------------------------------------------------

    @PrePersist
    protected void onPersist() {
        if (this.timestamp == null) {
            this.timestamp = Instant.now();
        }
    }

    // -------------------------------------------------------------------------
    // Constructeurs
    // -------------------------------------------------------------------------

    /** Constructeur JPA requis. */
    protected AuditLog() {}

    /**
     * Constructeur principal.
     * Le timestamp est automatiquement fixé à now() via @PrePersist
     * si non renseigné.
     */
    public AuditLog(String actorId,
                    String actorRole,
                    String action,
                    String resourceType,
                    String resourceId,
                    String ipAddress,
                    String details,
                    String hmacSignature) {
        this.actorId       = actorId;
        this.actorRole     = actorRole;
        this.action        = action;
        this.resourceType  = resourceType;
        this.resourceId    = resourceId;
        this.ipAddress     = ipAddress;
        this.details       = details;
        this.hmacSignature = hmacSignature;
        this.timestamp     = Instant.now();
    }

    // -------------------------------------------------------------------------
    // Getters (pas de setters : immutabilité voulue après création)
    // -------------------------------------------------------------------------

    public Long    getId()            { return id; }
    public String  getActorId()       { return actorId; }
    public String  getActorRole()     { return actorRole; }
    public String  getAction()        { return action; }
    public String  getResourceType()  { return resourceType; }
    public String  getResourceId()    { return resourceId; }
    public String  getIpAddress()     { return ipAddress; }
    public String  getDetails()       { return details; }
    public Instant getTimestamp()     { return timestamp; }
    public String  getHmacSignature() { return hmacSignature; }
}
