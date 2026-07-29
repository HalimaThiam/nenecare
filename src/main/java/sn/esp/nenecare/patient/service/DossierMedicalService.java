package sn.esp.nenecare.patient.service;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import sn.esp.nenecare.audit.service.AuditService;
import sn.esp.nenecare.common.exception.RessourceIntrouvableException;
import sn.esp.nenecare.common.security.UtilisateurCourant;
import sn.esp.nenecare.crypto.ProtectionDonneesService;
import sn.esp.nenecare.patient.dto.DossierMedicalRequest;
import sn.esp.nenecare.patient.dto.DossierMedicalResponse;
import sn.esp.nenecare.patient.model.DossierMedical;
import sn.esp.nenecare.patient.model.Patiente;
import sn.esp.nenecare.patient.model.StatutDossier;
import sn.esp.nenecare.patient.repository.DossierMedicalRepository;

/**
 * Dossiers medicaux de suivi de grossesse - US-07, US-08, US-09.
 *
 * Proprietaire : Amadou (patient / crypto).
 *
 * Cycle complet applique a chaque operation :
 *
 *   Creation / modification : DAC -> chiffrement AES-256-GCM -> signature
 *                             HMAC -> enregistrement -> audit
 *   Lecture                 : DAC -> dechiffrement -> verification HMAC -> audit
 *
 * La verification d'integrite n'interdit PAS la lecture d'un dossier suspect :
 * elle la signale (champ "integre" a false). Refuser d'afficher un dossier
 * medical parce que sa signature est cassee priverait le soignant d'une
 * information dont il a besoin ; l'avertir lui permet de la traiter avec
 * precaution et de declencher une verification.
 */
@Service
@RequiredArgsConstructor
public class DossierMedicalService {

    private static final String RESSOURCE = "DOSSIER_MEDICAL";

    private final DossierMedicalRepository dossierMedicalRepository;
    private final PatienteService patienteService;
    private final ProtectionDonneesService protection;
    private final ControleAccesDossier controleAcces;
    private final UtilisateurCourant utilisateurCourant;
    private final AuditService auditService;

    // =========================================================================
    // Lecture
    // =========================================================================

    @Transactional(readOnly = true)
    public List<DossierMedicalResponse> lister(String adresseIp) {
        List<DossierMedical> dossiers = controleAcces.estRestreintAuPortefeuille()
                ? dossierMedicalRepository
                    .findByPatienteGynecologueAssigneOrderByDateConsultationDesc(
                        utilisateurCourant.username())
                : dossierMedicalRepository.findAllByOrderByDateConsultationDesc();

        auditService.logAction(utilisateurCourant.username(), utilisateurCourant.role(),
                "DOSSIER_LIST", RESSOURCE,
                dossiers.size() + " dossier(s) medical(aux) liste(s)", adresseIp, true);

        return dossiers.stream().map(this::versReponse).toList();
    }

    /**
     * Consultation d'un dossier precis (US-07).
     *
     * @param motifUrgence renseigne uniquement pour un acces derogatoire (US-14)
     */
    @Transactional(readOnly = true)
    public DossierMedicalResponse consulter(Long id, String motifUrgence, String adresseIp) {
        DossierMedical dossier = chargerOuEchouer(id);
        controleAcces.verifierAcces(dossier.getPatiente(), RESSOURCE + "#" + id,
                motifUrgence, adresseIp);

        DossierMedicalResponse reponse = versReponse(dossier);

        auditService.logAction(utilisateurCourant.username(), utilisateurCourant.role(),
                "DOSSIER_READ", RESSOURCE + "#" + id,
                reponse.isIntegre()
                    ? "Consultation du dossier " + dossier.getNumeroDossier()
                    : "Consultation du dossier " + dossier.getNumeroDossier()
                      + " - SIGNATURE INVALIDE",
                adresseIp, true);

        return reponse;
    }

    @Transactional(readOnly = true)
    public List<DossierMedicalResponse> listerParPatiente(Long patienteId, String adresseIp) {
        Patiente patiente = patienteService.chargerOuEchouer(patienteId);
        controleAcces.verifierAcces(patiente, "PATIENTE#" + patienteId, adresseIp);

        List<DossierMedical> dossiers =
                dossierMedicalRepository.findByPatienteIdOrderByDateConsultationDesc(patienteId);

        auditService.logAction(utilisateurCourant.username(), utilisateurCourant.role(),
                "DOSSIER_LIST", "PATIENTE#" + patienteId,
                dossiers.size() + " dossier(s) de la patiente consulte(s)", adresseIp, true);

        return dossiers.stream().map(this::versReponse).toList();
    }

    // =========================================================================
    // Ecriture
    // =========================================================================

    @Transactional
    public DossierMedicalResponse creer(DossierMedicalRequest requete, String adresseIp) {
        Patiente patiente = patienteService.chargerOuEchouer(requete.getPatienteId());
        controleAcces.verifierAcces(patiente, "PATIENTE#" + patiente.getId(), adresseIp);

        DossierMedical dossier = DossierMedical.builder()
                .numeroDossier(genererNumero())
                .patiente(patiente)
                .dateConsultation(requete.getDateConsultation())
                .motif(requete.getMotif())
                // Chiffrement AVANT enregistrement (OS-03).
                .diagnosticChiffre(protection.chiffrer(requete.getDiagnostic()))
                .traitementChiffre(protection.chiffrer(requete.getTraitement()))
                .observationsChiffre(protection.chiffrer(requete.getObservations()))
                .statut(StatutDossier.ACTIF)
                .creePar(utilisateurCourant.username())
                .creeLe(LocalDateTime.now())
                .build();

        // Signature calculee sur le CLAIR : elle doit rester valable meme si
        // le dossier est un jour rechiffre avec une nouvelle cle.
        dossier.setSignatureHmac(protection.signer(messageASigner(dossier, requete)));

        DossierMedical enregistre = dossierMedicalRepository.save(dossier);

        auditService.logAction(utilisateurCourant.username(), utilisateurCourant.role(),
                "DOSSIER_CREATE", RESSOURCE + "#" + enregistre.getId(),
                "Creation du dossier " + enregistre.getNumeroDossier()
                    + " pour " + patiente.getNumeroPatiente(),
                adresseIp, true);

        return versReponse(enregistre);
    }

    @Transactional
    public DossierMedicalResponse modifier(Long id, DossierMedicalRequest requete,
                                           String adresseIp) {
        DossierMedical dossier = chargerOuEchouer(id);
        controleAcces.verifierAcces(dossier.getPatiente(), RESSOURCE + "#" + id, adresseIp);

        if (StatutDossier.ARCHIVE.equals(dossier.getStatut())) {
            throw new IllegalArgumentException(
                    "Ce dossier est archive : il ne peut plus etre modifie.");
        }

        dossier.setDateConsultation(requete.getDateConsultation());
        dossier.setMotif(requete.getMotif());
        dossier.setDiagnosticChiffre(protection.chiffrer(requete.getDiagnostic()));
        dossier.setTraitementChiffre(protection.chiffrer(requete.getTraitement()));
        dossier.setObservationsChiffre(protection.chiffrer(requete.getObservations()));
        dossier.setModifiePar(utilisateurCourant.username());
        dossier.setModifieLe(LocalDateTime.now());

        // La signature suit le contenu : sans ce recalcul, toute modification
        // legitime ferait apparaitre le dossier comme altere.
        dossier.setSignatureHmac(protection.signer(messageASigner(dossier, requete)));

        DossierMedical enregistre = dossierMedicalRepository.save(dossier);

        auditService.logAction(utilisateurCourant.username(), utilisateurCourant.role(),
                "DOSSIER_UPDATE", RESSOURCE + "#" + id,
                "Modification du dossier " + enregistre.getNumeroDossier(), adresseIp, true);

        return versReponse(enregistre);
    }

    /** US-09 : archivage. Le dossier reste consultable, il n'est jamais efface. */
    @Transactional
    public DossierMedicalResponse archiver(Long id, String adresseIp) {
        DossierMedical dossier = chargerOuEchouer(id);

        dossier.setStatut(StatutDossier.ARCHIVE);
        dossier.setModifiePar(utilisateurCourant.username());
        dossier.setModifieLe(LocalDateTime.now());

        DossierMedical enregistre = dossierMedicalRepository.save(dossier);

        auditService.logAction(utilisateurCourant.username(), utilisateurCourant.role(),
                "DOSSIER_ARCHIVE", RESSOURCE + "#" + id,
                "Archivage du dossier " + enregistre.getNumeroDossier(), adresseIp, true);

        return versReponse(enregistre);
    }

    // =========================================================================
    // Outils
    // =========================================================================

    private DossierMedical chargerOuEchouer(Long id) {
        return dossierMedicalRepository.findById(id)
                .orElseThrow(() -> RessourceIntrouvableException.dossier(id));
    }

    private DossierMedicalResponse versReponse(DossierMedical dossier) {
        String diagnostic   = protection.dechiffrer(dossier.getDiagnosticChiffre());
        String traitement   = protection.dechiffrer(dossier.getTraitementChiffre());
        String observations = protection.dechiffrer(dossier.getObservationsChiffre());

        boolean integre = protection.verifier(
                messageASigner(dossier, diagnostic, traitement, observations),
                dossier.getSignatureHmac());

        Patiente patiente = dossier.getPatiente();

        return DossierMedicalResponse.builder()
                .id(dossier.getId())
                .numeroDossier(dossier.getNumeroDossier())
                .patienteId(patiente.getId())
                .patienteNom(patiente.nomComplet())
                .patienteNumero(patiente.getNumeroPatiente())
                .dateConsultation(dossier.getDateConsultation())
                .motif(dossier.getMotif())
                .diagnostic(diagnostic)
                .traitement(traitement)
                .observations(observations)
                .statut(dossier.getStatut())
                .integre(integre)
                .creePar(dossier.getCreePar())
                .creeLe(dossier.getCreeLe())
                .modifiePar(dossier.getModifiePar())
                .modifieLe(dossier.getModifieLe())
                .build();
    }

    private String messageASigner(DossierMedical dossier, DossierMedicalRequest requete) {
        return messageASigner(dossier, requete.getDiagnostic(),
                requete.getTraitement(), requete.getObservations());
    }

    /**
     * Contenu signe : identite du dossier + rattachement a la patiente +
     * contenu medical en clair.
     *
     * Le numero de dossier ET l'identifiant de la patiente en font partie :
     * sans eux, il suffirait de recopier le bloc chiffre et la signature d'un
     * dossier dans la ligne d'une autre patiente pour obtenir un faux dossier
     * parfaitement valide.
     */
    private String messageASigner(DossierMedical dossier, String diagnostic,
                                  String traitement, String observations) {
        return String.join("|",
                nonNull(dossier.getNumeroDossier()),
                String.valueOf(dossier.getPatiente().getId()),
                String.valueOf(dossier.getDateConsultation()),
                nonNull(dossier.getMotif()),
                nonNull(diagnostic),
                nonNull(traitement),
                nonNull(observations));
    }

    private String nonNull(String valeur) {
        return valeur != null ? valeur : "";
    }

    private String genererNumero() {
        int annee = Year.now().getValue();
        long sequence = dossierMedicalRepository.count() + 1;
        String numero;
        do {
            numero = String.format("DM-%d-%04d", annee, sequence++);
        } while (dossierMedicalRepository.existsByNumeroDossier(numero));
        return numero;
    }
}
