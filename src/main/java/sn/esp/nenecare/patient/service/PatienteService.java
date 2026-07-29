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
import sn.esp.nenecare.patient.dto.PatienteRequest;
import sn.esp.nenecare.patient.dto.PatienteResponse;
import sn.esp.nenecare.patient.model.Patiente;
import sn.esp.nenecare.patient.repository.DossierMedicalRepository;
import sn.esp.nenecare.patient.repository.DossierNeonatalRepository;
import sn.esp.nenecare.patient.repository.PatienteRepository;
import sn.esp.nenecare.user.model.Role;

/**
 * Gestion des patientes (US-05, US-06).
 *
 * Proprietaire : Amadou (patient / crypto).
 *
 * Chaine appliquee a l'ecriture  : validation -> CHIFFREMENT -> save -> audit.
 * Chaine appliquee a la lecture  : DAC -> load -> DECHIFFREMENT -> audit.
 *
 * Le clair ne franchit jamais la frontiere de ce service en direction de la
 * base : les entites ne portent que des champs "...Chiffre".
 */
@Service
@RequiredArgsConstructor
public class PatienteService {

    private static final String RESSOURCE = "PATIENTE";

    private final PatienteRepository patienteRepository;
    private final DossierMedicalRepository dossierMedicalRepository;
    private final DossierNeonatalRepository dossierNeonatalRepository;
    private final ProtectionDonneesService protection;
    private final ControleAccesDossier controleAcces;
    private final UtilisateurCourant utilisateurCourant;
    private final AuditService auditService;

    // =========================================================================
    // Lecture
    // =========================================================================

    /**
     * Liste des patientes visibles par l'appelant.
     *
     * Le filtre DAC est pousse dans la requete SQL : un gynecologue ne charge
     * meme pas en memoire les patientes des autres.
     */
    @Transactional(readOnly = true)
    public List<PatienteResponse> lister(String adresseIp) {
        List<Patiente> patientes = controleAcces.estRestreintAuPortefeuille()
                ? patienteRepository.findByGynecologueAssigneOrderByNomAsc(
                        utilisateurCourant.username())
                : patienteRepository.findAllByOrderByNomAscPrenomAsc();

        auditService.logAction(utilisateurCourant.username(), utilisateurCourant.role(),
                "PATIENTE_LIST", RESSOURCE,
                patientes.size() + " patiente(s) consultee(s)", adresseIp, true);

        return patientes.stream().map(this::versReponse).toList();
    }

    /** Consultation d'une patiente precise, avec derogation d'urgence possible. */
    @Transactional(readOnly = true)
    public PatienteResponse consulter(Long id, String motifUrgence, String adresseIp) {
        Patiente patiente = chargerOuEchouer(id);
        controleAcces.verifierAcces(patiente, RESSOURCE + "#" + id, motifUrgence, adresseIp);

        auditService.logAction(utilisateurCourant.username(), utilisateurCourant.role(),
                "PATIENTE_READ", RESSOURCE + "#" + id,
                "Consultation de la fiche patiente", adresseIp, true);

        return versReponse(patiente);
    }

    // =========================================================================
    // Ecriture
    // =========================================================================

    @Transactional
    public PatienteResponse creer(PatienteRequest requete, String adresseIp) {
        Patiente patiente = Patiente.builder()
                .numeroPatiente(genererNumero())
                .nom(requete.getNom().trim())
                .prenom(requete.getPrenom().trim())
                .dateNaissance(requete.getDateNaissance())
                // Chiffrement AVANT save : la valeur en clair n'atteint jamais la base.
                .telephoneChiffre(protection.chiffrer(requete.getTelephone()))
                .adresseChiffre(protection.chiffrer(requete.getAdresse()))
                .groupeSanguinChiffre(protection.chiffrer(requete.getGroupeSanguin()))
                .antecedentsChiffre(protection.chiffrer(requete.getAntecedents()))
                .gynecologueAssigne(requete.getGynecologueAssigne())
                .creePar(utilisateurCourant.username())
                .creeLe(LocalDateTime.now())
                .build();

        Patiente enregistree = patienteRepository.save(patiente);

        auditService.logAction(utilisateurCourant.username(), utilisateurCourant.role(),
                "PATIENTE_CREATE", RESSOURCE + "#" + enregistree.getId(),
                "Creation de la fiche " + enregistree.getNumeroPatiente(), adresseIp, true);

        return versReponse(enregistree);
    }

    @Transactional
    public PatienteResponse modifier(Long id, PatienteRequest requete, String adresseIp) {
        Patiente patiente = chargerOuEchouer(id);
        controleAcces.verifierAcces(patiente, RESSOURCE + "#" + id, adresseIp);

        patiente.setNom(requete.getNom().trim());
        patiente.setPrenom(requete.getPrenom().trim());
        patiente.setDateNaissance(requete.getDateNaissance());
        patiente.setTelephoneChiffre(protection.chiffrer(requete.getTelephone()));
        patiente.setAdresseChiffre(protection.chiffrer(requete.getAdresse()));
        patiente.setGroupeSanguinChiffre(protection.chiffrer(requete.getGroupeSanguin()));
        patiente.setAntecedentsChiffre(protection.chiffrer(requete.getAntecedents()));
        patiente.setGynecologueAssigne(requete.getGynecologueAssigne());
        patiente.setModifieLe(LocalDateTime.now());

        Patiente enregistree = patienteRepository.save(patiente);

        auditService.logAction(utilisateurCourant.username(), utilisateurCourant.role(),
                "PATIENTE_UPDATE", RESSOURCE + "#" + id,
                "Modification de la fiche " + enregistree.getNumeroPatiente(), adresseIp, true);

        return versReponse(enregistree);
    }

    // =========================================================================
    // Acces interne (utilise par les services de dossiers)
    // =========================================================================

    @Transactional(readOnly = true)
    public Patiente chargerOuEchouer(Long id) {
        return patienteRepository.findById(id)
                .orElseThrow(() -> RessourceIntrouvableException.patiente(id));
    }

    // =========================================================================
    // Conversion
    // =========================================================================

    /**
     * Construit la vue API en dechiffrant les champs proteges.
     *
     * Le volet medical (groupe sanguin, antecedents) n'est pas dechiffre du
     * tout pour les roles non soignants : la donnee reste chiffree en memoire,
     * elle n'est pas simplement "masquee a l'affichage".
     */
    private PatienteResponse versReponse(Patiente patiente) {
        boolean voletMedical = aAccesAuVoletMedical();

        return PatienteResponse.builder()
                .id(patiente.getId())
                .numeroPatiente(patiente.getNumeroPatiente())
                .nom(patiente.getNom())
                .prenom(patiente.getPrenom())
                .dateNaissance(patiente.getDateNaissance())
                .telephone(protection.dechiffrer(patiente.getTelephoneChiffre()))
                .adresse(protection.dechiffrer(patiente.getAdresseChiffre()))
                .groupeSanguin(voletMedical
                        ? protection.dechiffrer(patiente.getGroupeSanguinChiffre()) : null)
                .antecedents(voletMedical
                        ? protection.dechiffrer(patiente.getAntecedentsChiffre()) : null)
                .gynecologueAssigne(patiente.getGynecologueAssigne())
                .voletMedicalVisible(voletMedical)
                .nombreDossiersMedicaux(dossierMedicalRepository
                        .findByPatienteIdOrderByDateConsultationDesc(patiente.getId()).size())
                .nombreDossiersNeonatals(dossierNeonatalRepository
                        .findByMereIdOrderByDateNaissanceDesc(patiente.getId()).size())
                .creeLe(patiente.getCreeLe())
                .build();
    }

    /** La secretaire gere l'accueil et les rendez-vous, pas le volet medical. */
    private boolean aAccesAuVoletMedical() {
        return !utilisateurCourant.aLeRole(Role.SECRETAIRE.name());
    }

    // =========================================================================
    // Numerotation
    // =========================================================================

    /**
     * Numero metier lisible : PAT-<annee>-<sequence>.
     *
     * La boucle couvre le cas ou deux inscriptions simultanees tomberaient sur
     * le meme numero ; l'index unique en base reste le garde-fou definitif.
     */
    private String genererNumero() {
        int annee = Year.now().getValue();
        long sequence = patienteRepository.count() + 1;
        String numero;
        do {
            numero = String.format("PAT-%d-%04d", annee, sequence++);
        } while (patienteRepository.existsByNumeroPatiente(numero));
        return numero;
    }
}
