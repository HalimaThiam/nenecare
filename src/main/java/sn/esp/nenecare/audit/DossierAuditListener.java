package sn.esp.nenecare.audit;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import sn.esp.nenecare.dossier.entity.DossierMedical;

/**
 * Listener JPA pour la piste d'audit des dossiers médicaux.
 *
 * Phase 1 : logs applicatifs structurés (SLF4J).
 * Phase 2 (TODO Hadja02) : persister en table audit_logs pour conformité.
 */
public class DossierAuditListener {

    private static final Logger log = LoggerFactory.getLogger(DossierAuditListener.class);

    @PrePersist
    public void onCreation(DossierMedical dossier) {
        log.info("AUDIT CREATE dossier patientId={} par={}",
            dossier.getPatient() != null ? dossier.getPatient().getId() : "?",
            currentUser());
    }

    @PreUpdate
    public void onUpdate(DossierMedical dossier) {
        log.info("AUDIT UPDATE dossier id={} par={}", dossier.getId(), currentUser());
    }

    @PreRemove
    public void onDelete(DossierMedical dossier) {
        log.warn("AUDIT DELETE dossier id={} patientId={} par={}",
            dossier.getId(),
            dossier.getPatient() != null ? dossier.getPatient().getId() : "?",
            currentUser());
    }

    private String currentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null ? auth.getName() : "ANONYMOUS";
        } catch (Exception e) {
            return "SYSTEM";
        }
    }
}
