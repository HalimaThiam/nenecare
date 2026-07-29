package sn.esp.nenecare.audit.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import sn.esp.nenecare.audit.model.AuditLog;
import sn.esp.nenecare.audit.repository.AuditLogRepository;
import sn.esp.nenecare.crypto.HmacService;

/**
 * Enregistrement et verification des entrees d'audit signees HMAC (OS-08).
 *
 * Proprietaire : Hadja (audit).
 *
 * Deux garanties importantes :
 *
 *  1. REQUIRES_NEW : l'entree d'audit est ecrite dans sa propre transaction.
 *     Si l'operation metier appelante echoue et fait un rollback, la trace
 *     subsiste - c'est precisement des tentatives echouees qu'on a besoin.
 *
 *  2. Une panne d'audit ne fait jamais echouer l'operation metier : l'erreur
 *     est journalisee dans les logs applicatifs, pas propagee a l'appelant.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final HmacService hmacService;
    private final String hmacKey;

    public AuditService(AuditLogRepository auditLogRepository,
                        HmacService hmacService,
                        @Value("${nenecare.audit.hmac-key}") String hmacKey) {
        this.auditLogRepository = auditLogRepository;
        this.hmacService = hmacService;
        this.hmacKey = hmacKey;
    }

    // =========================================================================
    // Ecriture
    // =========================================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logAction(String utilisateur, String role, String action,
                              String ressource, String details, String adresseIp,
                              Boolean succes) {
        try {
            // Troncature a la milliseconde AVANT signature : LocalDateTime.now()
            // porte des nanosecondes que PostgreSQL (microsecondes) et H2
            // arrondissent a l'enregistrement. Sans cela, l'horodatage relu ne
            // serait plus celui qui a ete signe et TOUTES les entrees
            // apparaitraient comme alterees apres un aller-retour en base.
            LocalDateTime maintenant = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
            String signature = hmacService.sign(
                    messageASigner(utilisateur, role, action, maintenant, ressource, succes),
                    hmacKey);

            AuditLog entree = AuditLog.builder()
                .timestamp(maintenant)
                .utilisateur(utilisateur)
                .role(role)
                .action(action)
                .ressource(ressource)
                .details(details)
                .adresseIp(adresseIp)
                .succes(succes)
                .signatureHmac(signature)
                .build();

            return auditLogRepository.save(entree);

        } catch (Exception e) {
            // Ne jamais casser l'operation metier a cause du journal : on trace
            // l'incident cote serveur pour qu'il soit visible en exploitation.
            log.error("Echec de l'enregistrement d'audit (action={}, utilisateur={})",
                      action, utilisateur, e);
            return null;
        }
    }

    // =========================================================================
    // Verification d'integrite
    // =========================================================================

    /** Recalcule la signature d'une entree et la compare a celle stockee. */
    public boolean verifierIntegrite(AuditLog entree) {
        try {
            String attendu = messageASigner(
                    entree.getUtilisateur(), entree.getRole(), entree.getAction(),
                    entree.getTimestamp(), entree.getRessource(), entree.getSucces());
            return hmacService.verify(attendu, hmacKey, entree.getSignatureHmac());
        } catch (Exception e) {
            log.warn("Verification d'integrite impossible pour l'entree {}", entree.getId(), e);
            return false;
        }
    }

    /**
     * Message signe : tous les champs porteurs de sens, separes par "|".
     *
     * Doit rester STRICTEMENT identique entre l'ecriture et la verification,
     * sinon toutes les entrees existantes apparaitraient comme alterees.
     */
    private String messageASigner(String utilisateur, String role, String action,
                                  LocalDateTime horodatage, String ressource, Boolean succes) {
        return String.join("|",
                nonNull(utilisateur),
                nonNull(role),
                nonNull(action),
                String.valueOf(horodatage),
                nonNull(ressource),
                Boolean.TRUE.equals(succes) ? "SUCCESS" : "FAILURE");
    }

    private String nonNull(String valeur) {
        return valeur != null ? valeur : "";
    }

    // =========================================================================
    // Consultation (US-16, US-17)
    // =========================================================================

    public List<AuditLog> getTousLesLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }

    public Optional<AuditLog> getLog(Long id) {
        return auditLogRepository.findById(id);
    }

    public List<AuditLog> getLogsByUtilisateur(String utilisateur) {
        return auditLogRepository.findByUtilisateur(utilisateur);
    }

    public List<AuditLog> getLogsByPeriode(LocalDateTime debut, LocalDateTime fin) {
        return auditLogRepository.findByTimestampBetween(debut, fin);
    }

    public List<AuditLog> getEchecs() {
        return auditLogRepository.findBySucces(false);
    }
}
