package com.nenecare.audit.service;

import com.nenecare.audit.model.AuditLog;
import com.nenecare.audit.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/**
 * Service d'audit NeneCare.
 *
 * Responsabilités :
 *  1. Calculer le HMAC-SHA256 d'une entrée d'audit.
 *  2. Persister les entrées dans la table isolée audit_log.
 *  3. Vérifier l'intégrité d'une entrée existante (revalidation à la lecture).
 *
 * La clé HMAC est injectée depuis application.properties (propriété nenecare.audit.hmac-key).
 * Elle doit être générée de manière aléatoire (≥ 256 bits) et stockée dans un secret manager,
 * jamais en clair dans le dépôt.
 *
 * Propagation REQUIRES_NEW : l'audit est persisté même si la transaction appelante
 * est annulée (rollback). Cela garantit la traçabilité des tentatives échouées.
 *
 * Sprint Alpha – feature/audit
 * Responsable : Hadja Mariama DIALLO
 */
@Service
public class AuditService {

    private static final String HMAC_ALGO    = "HmacSHA256";
    private static final String SEPARATOR    = "|";
    private static final String NULL_FIELD   = "NULL";

    private final AuditLogRepository repository;
    private final byte[]             hmacKeyBytes;

    public AuditService(AuditLogRepository repository,
                        @Value("${nenecare.audit.hmac-key}") String hmacKeyBase64) {
        this.repository   = repository;
        this.hmacKeyBytes = Base64.getDecoder().decode(hmacKeyBase64);
    }

    // =========================================================================
    // API publique – enregistrement d'événements
    // =========================================================================

    /**
     * Enregistre un événement d'audit.
     *
     * @param actorId      Identifiant de l'utilisateur
     * @param actorRole    Rôle de l'utilisateur (MEDECIN, SAGE_FEMME, ADMIN)
     * @param action       Code d'action (LOGIN_SUCCESS, DOSSIER_CREATE, …)
     * @param resourceType Type de ressource (PATIENTE, DOSSIER_MEDICAL, …)
     * @param resourceId   Identifiant de la ressource (peut être null)
     * @param ipAddress    Adresse IP de l'acteur
     * @param details      Détails libres (JSON ou texte, sans données sensibles)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog log(String actorId,
                        String actorRole,
                        String action,
                        String resourceType,
                        String resourceId,
                        String ipAddress,
                        String details) {

        Instant now    = Instant.now();
        String  hmac   = computeHmac(actorId, actorRole, action,
                                      resourceType, resourceId, now, details);

        AuditLog entry = new AuditLog(actorId, actorRole, action,
                                      resourceType, resourceId,
                                      ipAddress, details, hmac);

        return repository.save(entry);
    }

    /** Raccourci pour les événements sans ressource cible (ex : LOGIN). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog log(String actorId,
                        String actorRole,
                        String action,
                        String ipAddress,
                        String details) {
        return log(actorId, actorRole, action, null, null, ipAddress, details);
    }

    // =========================================================================
    // Vérification d'intégrité
    // =========================================================================

    /**
     * Vérifie l'intégrité d'une entrée d'audit en recalculant son HMAC.
     *
     * @param entry L'entrée à vérifier
     * @return true si la signature est valide, false en cas d'altération
     */
    public boolean verify(AuditLog entry) {
        String expected = computeHmac(
                entry.getActorId(),
                entry.getActorRole(),
                entry.getAction(),
                entry.getResourceType(),
                entry.getResourceId(),
                entry.getTimestamp(),
                entry.getDetails()
        );
        return Objects.equals(expected, entry.getHmacSignature());
    }

    // =========================================================================
    // Calcul HMAC
    // =========================================================================

    /**
     * Calcule le HMAC-SHA256 d'une entrée d'audit.
     *
     * Message signé (champs séparés par "|") :
     *   actorId | actorRole | action | resourceType | resourceId | timestamp | details
     *
     * Les champs null sont remplacés par la chaîne "NULL" pour éviter
     * les collisions entre null et chaîne vide.
     */
    private String computeHmac(String  actorId,
                                String  actorRole,
                                String  action,
                                String  resourceType,
                                String  resourceId,
                                Instant timestamp,
                                String  details) {
        try {
            String message = String.join(SEPARATOR,
                    safe(actorId),
                    safe(actorRole),
                    safe(action),
                    safe(resourceType),
                    safe(resourceId),
                    timestamp.toString(),
                    safe(details)
            );

            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(hmacKeyBytes, HMAC_ALGO));
            byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(rawHmac);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            // Ne devrait jamais arriver : HmacSHA256 est garanti par la JCA
            throw new IllegalStateException("Échec du calcul HMAC-SHA256", e);
        }
    }

    /** Remplace null par la sentinelle "NULL" pour la construction du message HMAC. */
    private String safe(String value) {
        return value != null ? value : NULL_FIELD;
    }
}
