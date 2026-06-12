package sn.esp.nenecare.nenecare.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sn.esp.nenecare.nenecare.crypto.HmacService;
import sn.esp.nenecare.nenecare.model.AuditLog;
import sn.esp.nenecare.nenecare.repository.AuditLogRepository;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final HmacService hmacService;

    // Clé HMAC fixe pour signer les entrées d'audit
    // En production, cette clé serait stockée de façon sécurisée
    private static final String HMAC_KEY =
        "bmVuZWNhcmUtaG1hYy1rZXktY2xpbmlxdWUtbWFyb3NlLTIwMjY=";

    public AuditLog logAction(String utilisateur, String role,
                        String action, String ressource,
                        String details, String adresseIp,
                        Boolean succes) {
    try {
        LocalDateTime now = LocalDateTime.now();

        // Données à signer — on utilise le timestamp fixé
        String dataToSign = utilisateur + "|" + role + "|" +
                            action + "|" + now + "|" +
                            (ressource != null ? ressource : "") + "|" +
                            (succes ? "SUCCESS" : "FAILURE");

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
        // On reconstruit exactement la même chaîne qu'à la création
        String dataToVerify = log.getUtilisateur() + "|" +
                            log.getRole() + "|" +
                            log.getAction() + "|" +
                            log.getTimestamp() + "|" +
                            (log.getRessource() != null ? log.getRessource() : "") + "|" +
                            (log.getSucces() ? "SUCCESS" : "FAILURE");

        return hmacService.verify(dataToVerify, HMAC_KEY, log.getSignatureHmac());
    } catch (Exception e) {
        return false;
    }
}


    // Récupère tous les logs
    public List<AuditLog> getTousLesLogs() {
        return auditLogRepository.findAll();
    }

    // Récupère les logs par utilisateur
    public List<AuditLog> getLogsByUtilisateur(String utilisateur) {
        return auditLogRepository.findByUtilisateur(utilisateur);
    }

    // Récupère les logs par période
    public List<AuditLog> getLogsByPeriode(LocalDateTime debut, LocalDateTime fin) {
        return auditLogRepository.findByTimestampBetween(debut, fin);
    }

    // Récupère les tentatives échouées
    public List<AuditLog> getEchecs() {
        return auditLogRepository.findBySucces(false);
    }
}