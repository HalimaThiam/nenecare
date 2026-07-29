package sn.esp.nenecare.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import sn.esp.nenecare.audit.model.AuditLog;
import sn.esp.nenecare.audit.repository.AuditLogRepository;
import sn.esp.nenecare.audit.service.AuditService;

/**
 * Integrite du journal d'audit signe HMAC-SHA256 (OS-08).
 *
 * C'est le coeur de la valeur probante du journal : si une entree est
 * modifiee en base, la verification doit le detecter.
 */
@SpringBootTest
class AuditIntegriteTest {

    @Autowired private AuditService auditService;
    @Autowired private AuditLogRepository auditLogRepository;

    @BeforeEach
    void viderJournal() {
        auditLogRepository.deleteAll();
    }

    @Test
    @DisplayName("Une entree fraichement ecrite est signee et verifiable")
    void entreeIntegre() {
        AuditLog entree = auditService.logAction(
                "gyneco.test", "GYNECOLOGUE", "DOSSIER_READ",
                "DOSSIER_MEDICAL#42", "Consultation du dossier", "10.0.0.5", true);

        assertThat(entree).isNotNull();
        assertThat(entree.getSignatureHmac()).isNotBlank();
        assertThat(auditService.verifierIntegrite(entree)).isTrue();
    }

    @Test
    @DisplayName("Modifier l'action d'une entree rend la signature invalide")
    void alterationDetectee() {
        AuditLog entree = auditService.logAction(
                "gyneco.test", "GYNECOLOGUE", "DOSSIER_DELETE",
                "DOSSIER_MEDICAL#42", "Suppression", "10.0.0.5", true);

        // Un attaquant maquille une suppression en simple consultation.
        entree.setAction("DOSSIER_READ");

        assertThat(auditService.verifierIntegrite(entree)).isFalse();
    }

    @Test
    @DisplayName("Changer l'utilisateur d'une entree rend la signature invalide")
    void changementDActeurDetecte() {
        AuditLog entree = auditService.logAction(
                "gyneco.test", "GYNECOLOGUE", "DOSSIER_READ",
                null, null, "10.0.0.5", true);

        entree.setUtilisateur("quelquun.dautre");

        assertThat(auditService.verifierIntegrite(entree)).isFalse();
    }

    @Test
    @DisplayName("Transformer un echec en succes rend la signature invalide")
    void changementDIssueDetecte() {
        AuditLog entree = auditService.logAction(
                "inconnu", "INCONNU", "LOGIN_FAILURE",
                null, "Echec d'authentification", "10.0.0.5", false);

        entree.setSucces(true);

        assertThat(auditService.verifierIntegrite(entree)).isFalse();
    }

    @Test
    @DisplayName("Une signature videe ou remplacee est rejetee")
    void signatureFalsifieeRejetee() {
        AuditLog entree = auditService.logAction(
                "gyneco.test", "GYNECOLOGUE", "DOSSIER_READ",
                null, null, "10.0.0.5", true);

        entree.setSignatureHmac("c2lnbmF0dXJlLWludmVudGVl");
        assertThat(auditService.verifierIntegrite(entree)).isFalse();

        entree.setSignatureHmac(null);
        assertThat(auditService.verifierIntegrite(entree)).isFalse();
    }

    @Test
    @DisplayName("L'entree relue depuis la base reste verifiable")
    void integriteApresRelectureEnBase() {
        AuditLog ecrite = auditService.logAction(
                "sagefemme.test", "SAGE_FEMME", "LOGIN_SUCCESS",
                null, "Connexion reussie", "192.168.1.21", true);

        AuditLog relue = auditLogRepository.findById(ecrite.getId()).orElseThrow();

        assertThat(auditService.verifierIntegrite(relue)).isTrue();
    }
}
