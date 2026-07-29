package sn.esp.nenecare.patient.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import sn.esp.nenecare.audit.service.AuditService;
import sn.esp.nenecare.common.security.UtilisateurCourant;
import sn.esp.nenecare.patient.model.Patiente;
import sn.esp.nenecare.user.model.Role;

/**
 * Controle d'acces discretionnaire (DAC) aux dossiers - US-07, US-14.
 *
 * Proprietaire : Amadou (patient), regle validee avec Halima (securite).
 *
 * Le RBAC (@PreAuthorize) repond a "ce role a-t-il le droit de lire des
 * dossiers ?". Le DAC repond a "CE dossier-la, precisement ?". Les deux sont
 * necessaires : sans DAC, tout gynecologue de la clinique lirait le dossier
 * de toutes les patientes, ce qui est exactement la fuite qu'on cherche a
 * empecher.
 *
 * REGLE : un GYNECOLOGUE n'accede qu'aux patientes dont il est le referent.
 * Les autres roles soignants (sage-femme, pediatre, infirmier) interviennent
 * sur l'ensemble du service et ne sont pas restreints par ce filtre ; c'est
 * le RBAC qui limite ce qu'ils peuvent faire.
 *
 * DEROGATION D'URGENCE (US-14) : un gynecologue peut forcer l'acces a une
 * patiente qui ne lui est pas assignee - une urgence obstetricale n'attend
 * pas une reaffectation administrative. Cet acces n'est jamais silencieux :
 * il exige un motif ecrit et produit une entree d'audit dediee.
 */
@Component
@RequiredArgsConstructor
public class ControleAccesDossier {

    public static final String ACTION_ACCES_URGENCE = "ACCES_URGENCE";
    public static final String ACTION_ACCES_REFUSE = "ACCES_REFUSE";

    private final UtilisateurCourant utilisateurCourant;
    private final AuditService auditService;

    /**
     * Verifie que l'appelant a le droit d'acceder aux dossiers de cette
     * patiente, et trace la derogation le cas echeant.
     *
     * @param motifUrgence motif de la derogation, ou null pour un acces normal
     * @throws AccessDeniedException si l'acces n'est pas permis
     */
    public void verifierAcces(Patiente patiente, String ressource,
                              String motifUrgence, String adresseIp) {

        if (estAutorise(patiente)) {
            return;
        }

        String utilisateur = utilisateurCourant.username();
        String role = utilisateurCourant.role();

        // Acces d'urgence : autorise, mais laisse une trace impossible a manquer.
        if (motifUrgence != null && !motifUrgence.isBlank()) {
            auditService.logAction(utilisateur, role, ACTION_ACCES_URGENCE, ressource,
                    "Acces derogatoire a une patiente non assignee. Motif declare : "
                            + motifUrgence.trim(),
                    adresseIp, true);
            return;
        }

        auditService.logAction(utilisateur, role, ACTION_ACCES_REFUSE, ressource,
                "Tentative d'acces a une patiente non assignee", adresseIp, false);

        throw new AccessDeniedException(
                "Cette patiente n'est pas dans votre portefeuille. "
                + "En cas d'urgence, renouvelez la demande en precisant un motif.");
    }

    /** Acces normal, sans derogation possible : utilise pour l'ecriture. */
    public void verifierAcces(Patiente patiente, String ressource, String adresseIp) {
        verifierAcces(patiente, ressource, null, adresseIp);
    }

    /** Vrai si l'appelant peut voir cette patiente sans derogation. */
    public boolean estAutorise(Patiente patiente) {
        if (!estRestreintAuPortefeuille()) {
            return true;
        }
        return patiente.getGynecologueAssigne() != null
                && patiente.getGynecologueAssigne().equals(utilisateurCourant.username());
    }

    /** Seul le gynecologue est limite a son portefeuille de patientes. */
    public boolean estRestreintAuPortefeuille() {
        return utilisateurCourant.aLeRole(Role.GYNECOLOGUE.name());
    }
}
