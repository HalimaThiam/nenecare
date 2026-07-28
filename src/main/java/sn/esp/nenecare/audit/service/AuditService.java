package sn.esp.nenecare.audit.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sn.esp.nenecare.audit.model.AuditLog;
import sn.esp.nenecare.audit.repository.AuditLogRepository;
import sn.esp.nenecare.crypto.HmacService;

/**
 * Enregistrement et verification des entrees d'audit signees HMAC (OS-08).
 *
 * Proprietaire : Hadja (audit).
 * NOTE : la cle HMAC est ici en dur pour la base ; a externaliser (variable
 * d'environnement / coffre) avant la soumission finale.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final HmacService hmacService;

    private static final String HMAC_KEY =
        "bmVuZWNhcmUtaG1hYy1rZXktY2xpbmlxdWUtbWFyb3NlLTIwMjY=";

    public AuditLog logAction(String utilisateur, String role, String action,
                              String ressource, String details, String adresseIp,
                              Boolean succes) {
        try {
            LocalDateTime now = LocalDateTime.now();
            String dataToSign = utilisateur + "|" + role + "|" + action + "|" + now + "|"
                + (ressource != null ? ressource : "") + "|"
                + (succes ? "SUCCESS" : "FAILURE");

            String signature = hmacService.sign(dataToSign, HMAC_KEY);

            AuditLog log = AuditLog.builder()
                .timestamp(now)
                .utilisateur(utilisateur)
                .role(role)
                .action(action)
                .ressource(ressource)
                .details(details)
                .adresseIp(adresseIp)
                .succes(succes)
                .signatureHmac(signature)
                .build();

            return auditLogRepository.save(log);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'enregistrement de l'audit", e);
        }
    }

    public boolean verifierIntegrite(AuditLog log) {
        try {
            String dataToVerify = log.getUtilisateur() + "|" + log.getRole() + "|"
                + log.getAction() + "|" + log.getTimestamp() + "|"
                + (log.getRessource() != null ? log.getRessource() : "") + "|"
                + (log.getSucces() ? "SUCCESS" : "FAILURE");
            return hmacService.verify(dataToVerify, HMAC_KEY, log.getSignatureHmac());
        } catch (Exception e) {
            return false;
        }
    }

    public List<AuditLog> getTousLesLogs() {
        return auditLogRepository.findAll();
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
