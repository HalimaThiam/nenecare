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
import sn.esp.nenecare.patient.dto.DossierNeonatalRequest;
import sn.esp.nenecare.patient.dto.DossierNeonatalResponse;
import sn.esp.nenecare.patient.model.DossierNeonatal;
import sn.esp.nenecare.patient.model.Patiente;
import sn.esp.nenecare.patient.model.StatutDossier;
import sn.esp.nenecare.patient.repository.DossierNeonatalRepository;

/**
 * Dossiers des nouveau-nes - US-10 a US-13.
 *
 * Proprietaire : Amadou (patient / crypto).
 *
 * Meme chaine de protection que les dossiers medicaux : chiffrement AES-256-GCM
 * du contenu clinique, signature HMAC de l'ensemble, audit de chaque acces.
 *
 * Particularite : la LIAISON MERE-ENFANT (OS-06) entre dans le message signe.
 * Rattacher a posteriori le dossier d'un nouveau-ne a une autre mere casserait
 * donc la signature - une erreur de rattachement en maternite n'est pas un
 * detail administratif.
 */
@Service
@RequiredArgsConstructor
public class DossierNeonatalService {

    private static final String RESSOURCE = "DOSSIER_NEONATAL";

    private final DossierNeonatalRepository dossierNeonatalRepository;
    private final PatienteService patienteService;
    private final ProtectionDonneesService protection;
    private final ControleAccesDossier controleAcces;
    private final UtilisateurCourant utilisateurCourant;
    private final AuditService auditService;

    // =========================================================================
    // Lecture
    // =========================================================================

    @Transactional(readOnly = true)
    public List<DossierNeonatalResponse> lister(String adresseIp) {
        List<DossierNeonatal> dossiers =
                dossierNeonatalRepository.findAllByOrderByDateNaissanceDesc();

        // Le pediatre et la sage-femme suivent tous les nouveau-nes du service ;
        // le filtre du portefeuille ne s'applique qu'au gynecologue, via la mere.
        if (controleAcces.estRestreintAuPortefeuille()) {
            dossiers = dossiers.stream()
                    .filter(dossier -> controleAcces.estAutorise(dossier.getMere()))
                    .toList();
        }

        auditService.logAction(utilisateurCourant.username(), utilisateurCourant.role(),
                "NEONATAL_LIST", RESSOURCE,
                dossiers.size() + " dossier(s) neonatal(s) liste(s)", adresseIp, true);

        return dossiers.stream().map(this::versReponse).toList();
    }

    @Transactional(readOnly = true)
    public DossierNeonatalResponse consulter(Long id, String motifUrgence, String adresseIp) {
        DossierNeonatal dossier = chargerOuEchouer(id);
        controleAcces.verifierAcces(dossier.getMere(), RESSOURCE + "#" + id,
                motifUrgence, adresseIp);

        DossierNeonatalResponse reponse = versReponse(dossier);

        auditService.logAction(utilisateurCourant.username(), utilisateurCourant.role(),
                "NEONATAL_READ", RESSOURCE + "#" + id,
                reponse.isIntegre()
                    ? "Consultation du dossier " + dossier.getNumeroDossier()
                    : "Consultation du dossier " + dossier.getNumeroDossier()
                      + " - SIGNATURE INVALIDE",
                adresseIp, true);

        return reponse;
    }

    /** Tous les enfants d'une meme mere - vue mere-enfant (OS-06). */
    @Transactional(readOnly = true)
    public List<DossierNeonatalResponse> listerParMere(Long mereId, String adresseIp) {
        Patiente mere = patienteService.chargerOuEchouer(mereId);
        controleAcces.verifierAcces(mere, "PATIENTE#" + mereId, adresseIp);

        List<DossierNeonatal> dossiers =
                dossierNeonatalRepository.findByMereIdOrderByDateNaissanceDesc(mereId);

        auditService.logAction(utilisateurCourant.username(), utilisateurCourant.role(),
                "NEONATAL_LIST", "PATIENTE#" + mereId,
                dossiers.size() + " nouveau-ne(s) rattache(s) a la mere", adresseIp, true);

        return dossiers.stream().map(this::versReponse).toList();
    }

    // =========================================================================
    // Ecriture
    // =========================================================================

    @Transactional
    public DossierNeonatalResponse creer(DossierNeonatalRequest requete, String adresseIp) {
        Patiente mere = patienteService.chargerOuEchouer(requete.getMereId());
        controleAcces.verifierAcces(mere, "PATIENTE#" + mere.getId(), adresseIp);

        DossierNeonatal dossier = DossierNeonatal.builder()
                .numeroDossier(genererNumero())
                .mere(mere)
                .nomBebe(requete.getNomBebe().trim())
                .sexe(requete.getSexe())
                .dateNaissance(requete.getDateNaissance())
                .poidsGrammes(requete.getPoidsGrammes())
                .tailleCm(requete.getTailleCm())
                .scoreApgar(requete.getScoreApgar())
                .diagnosticChiffre(protection.chiffrer(requete.getDiagnostic()))
                .observationsChiffre(protection.chiffrer(requete.getObservations()))
                .statut(StatutDossier.ACTIF)
                .creePar(utilisateurCourant.username())
                .creeLe(LocalDateTime.now())
                .build();

        dossier.setSignatureHmac(protection.signer(messageASigner(dossier,
                requete.getDiagnostic(), requete.getObservations())));

        DossierNeonatal enregistre = dossierNeonatalRepository.save(dossier);

        auditService.logAction(utilisateurCourant.username(), utilisateurCourant.role(),
                "NEONATAL_CREATE", RESSOURCE + "#" + enregistre.getId(),
                "Creation du dossier " + enregistre.getNumeroDossier()
                    + " rattache a la mere " + mere.getNumeroPatiente(),
                adresseIp, true);

        return versReponse(enregistre);
    }

    @Transactional
    public DossierNeonatalResponse modifier(Long id, DossierNeonatalRequest requete,
                                            String adresseIp) {
        DossierNeonatal dossier = chargerOuEchouer(id);
        controleAcces.verifierAcces(dossier.getMere(), RESSOURCE + "#" + id, adresseIp);

        if (StatutDossier.ARCHIVE.equals(dossier.getStatut())) {
            throw new IllegalArgumentException(
                    "Ce dossier est archive : il ne peut plus etre modifie.");
        }

        dossier.setNomBebe(requete.getNomBebe().trim());
        dossier.setSexe(requete.getSexe());
        dossier.setDateNaissance(requete.getDateNaissance());
        dossier.setPoidsGrammes(requete.getPoidsGrammes());
        dossier.setTailleCm(requete.getTailleCm());
        dossier.setScoreApgar(requete.getScoreApgar());
        dossier.setDiagnosticChiffre(protection.chiffrer(requete.getDiagnostic()));
        dossier.setObservationsChiffre(protection.chiffrer(requete.getObservations()));
        dossier.setModifiePar(utilisateurCourant.username());
        dossier.setModifieLe(LocalDateTime.now());

        dossier.setSignatureHmac(protection.signer(messageASigner(dossier,
                requete.getDiagnostic(), requete.getObservations())));

        DossierNeonatal enregistre = dossierNeonatalRepository.save(dossier);

        auditService.logAction(utilisateurCourant.username(), utilisateurCourant.role(),
                "NEONATAL_UPDATE", RESSOURCE + "#" + id,
                "Modification du dossier " + enregistre.getNumeroDossier(), adresseIp, true);

        return versReponse(enregistre);
    }

    // =========================================================================
    // Outils
    // =========================================================================

    private DossierNeonatal chargerOuEchouer(Long id) {
        return dossierNeonatalRepository.findById(id)
                .orElseThrow(() -> RessourceIntrouvableException.dossier(id));
    }

    private DossierNeonatalResponse versReponse(DossierNeonatal dossier) {
        String diagnostic   = protection.dechiffrer(dossier.getDiagnosticChiffre());
        String observations = protection.dechiffrer(dossier.getObservationsChiffre());

        boolean integre = protection.verifier(
                messageASigner(dossier, diagnostic, observations),
                dossier.getSignatureHmac());

        Patiente mere = dossier.getMere();

        return DossierNeonatalResponse.builder()
                .id(dossier.getId())
                .numeroDossier(dossier.getNumeroDossier())
                .mereId(mere.getId())
                .mereNom(mere.nomComplet())
                .mereNumero(mere.getNumeroPatiente())
                .nomBebe(dossier.getNomBebe())
                .sexe(dossier.getSexe())
                .dateNaissance(dossier.getDateNaissance())
                .poidsGrammes(dossier.getPoidsGrammes())
                .tailleCm(dossier.getTailleCm())
                .scoreApgar(dossier.getScoreApgar())
                .diagnostic(diagnostic)
                .observations(observations)
                .statut(dossier.getStatut())
                .integre(integre)
                .creePar(dossier.getCreePar())
                .creeLe(dossier.getCreeLe())
                .modifiePar(dossier.getModifiePar())
                .modifieLe(dossier.getModifieLe())
                .build();
    }

    /** L'identifiant de la mere entre dans la signature : liaison scellee (OS-06). */
    private String messageASigner(DossierNeonatal dossier, String diagnostic,
                                  String observations) {
        return String.join("|",
                nonNull(dossier.getNumeroDossier()),
                String.valueOf(dossier.getMere().getId()),
                nonNull(dossier.getNomBebe()),
                nonNull(dossier.getSexe()),
                String.valueOf(dossier.getDateNaissance()),
                String.valueOf(dossier.getPoidsGrammes()),
                String.valueOf(dossier.getTailleCm()),
                String.valueOf(dossier.getScoreApgar()),
                nonNull(diagnostic),
                nonNull(observations));
    }

    private String nonNull(String valeur) {
        return valeur != null ? valeur : "";
    }

    private String genererNumero() {
        int annee = Year.now().getValue();
        long sequence = dossierNeonatalRepository.count() + 1;
        String numero;
        do {
            numero = String.format("DN-%d-%04d", annee, sequence++);
        } while (dossierNeonatalRepository.existsByNumeroDossier(numero));
        return numero;
    }
}
