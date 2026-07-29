package sn.esp.nenecare.patient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import sn.esp.nenecare.audit.model.AuditLog;
import sn.esp.nenecare.audit.repository.AuditLogRepository;
import sn.esp.nenecare.crypto.ProtectionDonneesService;
import sn.esp.nenecare.patient.model.DossierMedical;
import sn.esp.nenecare.patient.model.Patiente;
import sn.esp.nenecare.patient.repository.DossierMedicalRepository;
import sn.esp.nenecare.patient.repository.DossierNeonatalRepository;
import sn.esp.nenecare.patient.repository.PatienteRepository;

/**
 * Parcours complet du module Dossiers : US-05 a US-14, OS-03, OS-05, OS-06,
 * OS-07, OS-08.
 *
 * Sans @Transactional, comme AuthenticationIT : le service d'audit ecrit dans
 * une transaction separee (REQUIRES_NEW) et ses entrees ne seraient pas
 * visibles autrement. Le nettoyage est donc explicite.
 *
 * @WithMockUser fournit l'identite ET le role : c'est exactement ce que
 * JwtAuthFilter place dans le SecurityContext en production, donc les regles
 * RBAC et DAC testees ici sont celles qui s'appliqueront reellement.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DossiersIT {

    private static final String GYNECO = "gyneco.un";
    private static final String GYNECO_AUTRE = "gyneco.deux";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PatienteRepository patienteRepository;
    @Autowired private DossierMedicalRepository dossierMedicalRepository;
    @Autowired private DossierNeonatalRepository dossierNeonatalRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private ProtectionDonneesService protection;

    private Long patienteDuGyneco;
    private Long patienteDuConfrere;

    @BeforeEach
    void preparerJeuDeDonnees() {
        dossierNeonatalRepository.deleteAll();
        dossierMedicalRepository.deleteAll();
        patienteRepository.deleteAll();
        auditLogRepository.deleteAll();

        patienteDuGyneco = patienteRepository.save(Patiente.builder()
                .numeroPatiente("PAT-TEST-0001")
                .nom("NDIAYE").prenom("Aissatou")
                .dateNaissance(LocalDate.of(1994, 3, 12))
                .telephoneChiffre(protection.chiffrer("+221 77 512 44 08"))
                .groupeSanguinChiffre(protection.chiffrer("O+"))
                .antecedentsChiffre(protection.chiffrer("Deuxieme grossesse"))
                .gynecologueAssigne(GYNECO)
                .build()).getId();

        patienteDuConfrere = patienteRepository.save(Patiente.builder()
                .numeroPatiente("PAT-TEST-0002")
                .nom("BA").prenom("Mariama")
                .gynecologueAssigne(GYNECO_AUTRE)
                .build()).getId();
    }

    // =========================================================================
    // OS-03 : chiffrement des donnees medicales au repos
    // =========================================================================

    @Test
    @WithMockUser(username = GYNECO, roles = "GYNECOLOGUE")
    @DisplayName("OS-03 : le diagnostic n'apparait jamais en clair dans la base")
    void contenuMedicalChiffreEnBase() throws Exception {
        String diagnostic = "Grossesse evolutive, presentation cephalique";

        mockMvc.perform(post("/api/dossiers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsDossier(patienteDuGyneco, diagnostic)))
                .andExpect(status().isOk());

        DossierMedical enBase = dossierMedicalRepository.findAll().get(0);

        // La colonne stockee ne contient pas le texte d'origine...
        assertThat(enBase.getDiagnosticChiffre())
                .isNotNull()
                .doesNotContain(diagnostic)
                .doesNotContain("Grossesse");

        // ...mais le dechiffrement rend bien la valeur saisie.
        assertThat(protection.dechiffrer(enBase.getDiagnosticChiffre())).isEqualTo(diagnostic);
    }

    @Test
    @WithMockUser(username = GYNECO, roles = "GYNECOLOGUE")
    @DisplayName("OS-03 : deux dossiers au contenu identique donnent deux chiffres differents")
    void chiffrementNonDeterministe() throws Exception {
        String meme = "Diagnostic strictement identique";

        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON)
                .content(corpsDossier(patienteDuGyneco, meme))).andExpect(status().isOk());
        mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON)
                .content(corpsDossier(patienteDuGyneco, meme))).andExpect(status().isOk());

        List<DossierMedical> dossiers = dossierMedicalRepository.findAll();
        assertThat(dossiers).hasSize(2);

        // Un IV aleatoire par chiffrement : sans cela, comparer deux lignes de
        // la table revelerait que deux patientes portent le meme diagnostic.
        assertThat(dossiers.get(0).getDiagnosticChiffre())
                .isNotEqualTo(dossiers.get(1).getDiagnosticChiffre());
    }

    // =========================================================================
    // OS-07 : integrite HMAC
    // =========================================================================

    @Test
    @WithMockUser(username = GYNECO, roles = "GYNECOLOGUE")
    @DisplayName("OS-07 : un dossier intact est signale comme integre")
    void dossierIntactEstIntegre() throws Exception {
        Long id = creerDossier(patienteDuGyneco, "Diagnostic initial");

        mockMvc.perform(get("/api/dossiers/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donnees.integre").value(true));
    }

    @Test
    @WithMockUser(username = GYNECO, roles = "GYNECOLOGUE")
    @DisplayName("OS-07 : une modification faite hors de l'application est detectee")
    void alterationHorsApplicationDetectee() throws Exception {
        Long id = creerDossier(patienteDuGyneco, "Diagnostic initial");

        // Simule un acces SQL direct : le contenu est remplace par un autre
        // bloc valide, mais la signature n'est pas recalculee.
        DossierMedical dossier = dossierMedicalRepository.findById(id).orElseThrow();
        dossier.setDiagnosticChiffre(protection.chiffrer("Diagnostic falsifie"));
        dossierMedicalRepository.save(dossier);

        mockMvc.perform(get("/api/dossiers/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donnees.integre").value(false))
                // Le contenu reste lisible : le soignant est averti, pas prive
                // d'une information dont il peut avoir un besoin urgent.
                .andExpect(jsonPath("$.donnees.diagnostic").value("Diagnostic falsifie"));
    }

    @Test
    @WithMockUser(username = GYNECO, roles = "GYNECOLOGUE")
    @DisplayName("OS-07 : une modification legitime laisse le dossier integre")
    void modificationLegitimeRecalculeLaSignature() throws Exception {
        Long id = creerDossier(patienteDuGyneco, "Diagnostic initial");

        mockMvc.perform(put("/api/dossiers/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsDossier(patienteDuGyneco, "Diagnostic revise")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donnees.integre").value(true));
    }

    // =========================================================================
    // OS-05 : RBAC
    // =========================================================================

    @Test
    @WithMockUser(username = "secretaire", roles = "SECRETAIRE")
    @DisplayName("OS-05 : la secretaire n'accede pas aux dossiers medicaux")
    void secretaireRefuseeSurLesDossiers() throws Exception {
        mockMvc.perform(get("/api/dossiers")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/dossiers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsDossier(patienteDuGyneco, "Tentative")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "secretaire", roles = "SECRETAIRE")
    @DisplayName("OS-05 : la secretaire voit la fiche patiente mais pas son volet medical")
    void secretaireNeRecoitPasLeVoletMedical() throws Exception {
        mockMvc.perform(get("/api/patientes/" + patienteDuGyneco))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donnees.nom").value("NDIAYE"))
                .andExpect(jsonPath("$.donnees.voletMedicalVisible").value(false))
                // Le champ n'est pas masque a l'affichage : il n'est pas transmis.
                .andExpect(jsonPath("$.donnees.groupeSanguin").doesNotExist())
                .andExpect(jsonPath("$.donnees.antecedents").doesNotExist());
    }

    @Test
    @WithMockUser(username = "gyneco.un", roles = "GYNECOLOGUE")
    @DisplayName("OS-05 : le gynecologue recoit le volet medical de sa patiente")
    void soignantRecoitLeVoletMedical() throws Exception {
        mockMvc.perform(get("/api/patientes/" + patienteDuGyneco))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donnees.voletMedicalVisible").value(true))
                .andExpect(jsonPath("$.donnees.groupeSanguin").value("O+"));
    }

    @Test
    @WithMockUser(username = "pediatre", roles = "PEDIATRE")
    @DisplayName("OS-05 : le pediatre n'accede pas au suivi de grossesse")
    void pediatreRefuseSurLeSuiviDeGrossesse() throws Exception {
        mockMvc.perform(get("/api/dossiers")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "gyneco.un", roles = "GYNECOLOGUE")
    @DisplayName("OS-05 : le gynecologue n'accede pas aux dossiers neonatals")
    void gynecologueRefuseSurLeNeonatal() throws Exception {
        mockMvc.perform(get("/api/neonatals")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = GYNECO, roles = "GYNECOLOGUE")
    @DisplayName("OS-05 : seul l'administrateur archive un dossier")
    void archivageReserveALAdministrateur() throws Exception {
        Long id = creerDossier(patienteDuGyneco, "Diagnostic");
        mockMvc.perform(put("/api/dossiers/" + id + "/archiver"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // US-07 / US-14 : DAC et acces d'urgence
    // =========================================================================

    @Test
    @WithMockUser(username = GYNECO, roles = "GYNECOLOGUE")
    @DisplayName("US-07 : la liste d'un gynecologue ne contient que ses patientes")
    void listeFiltreeParPortefeuille() throws Exception {
        mockMvc.perform(get("/api/patientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donnees.length()").value(1))
                .andExpect(jsonPath("$.donnees[0].numeroPatiente").value("PAT-TEST-0001"));
    }

    @Test
    @WithMockUser(username = GYNECO, roles = "GYNECOLOGUE")
    @DisplayName("US-07 : le dossier d'une patiente d'un confrere est refuse")
    void accesRefuseHorsPortefeuille() throws Exception {
        mockMvc.perform(get("/api/patientes/" + patienteDuConfrere))
                .andExpect(status().isForbidden());

        assertThat(actions()).contains("ACCES_REFUSE");
        assertThat(auditLogRepository.findAll().stream()
                        .filter(e -> "ACCES_REFUSE".equals(e.getAction()))
                        .allMatch(e -> Boolean.FALSE.equals(e.getSucces())))
                .isTrue();
    }

    @Test
    @WithMockUser(username = GYNECO, roles = "GYNECOLOGUE")
    @DisplayName("US-14 : l'acces d'urgence est autorise et laisse une trace motivee")
    void accesUrgenceAutoriseEtTrace() throws Exception {
        String motif = "Hemorragie du post-partum, referent absent";

        mockMvc.perform(get("/api/patientes/" + patienteDuConfrere).param("motifUrgence", motif))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donnees.numeroPatiente").value("PAT-TEST-0002"));

        AuditLog trace = auditLogRepository.findAll().stream()
                .filter(e -> "ACCES_URGENCE".equals(e.getAction()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Aucune trace ACCES_URGENCE"));

        assertThat(trace.getUtilisateur()).isEqualTo(GYNECO);
        assertThat(trace.getDetails()).contains(motif);
    }

    // =========================================================================
    // OS-06 : liaison mere-enfant
    // =========================================================================

    @Test
    @WithMockUser(username = "pediatre", roles = "PEDIATRE")
    @DisplayName("OS-06 : un dossier neonatal sans mere est refuse")
    void dossierNeonatalSansMereRefuse() throws Exception {
        String corps = objectMapper.writeValueAsString(Map.of(
                "nomBebe", "Ousmane NDIAYE",
                "dateNaissance", LocalDate.now().toString()));

        mockMvc.perform(post("/api/neonatals")
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "pediatre", roles = "PEDIATRE")
    @DisplayName("OS-06 : le nouveau-ne reste rattache a sa mere et le lien est signe")
    void liaisonMereEnfantEtablieEtSignee() throws Exception {
        mockMvc.perform(post("/api/neonatals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsNeonatal(patienteDuGyneco)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donnees.mereId").value(patienteDuGyneco))
                .andExpect(jsonPath("$.donnees.mereNumero").value("PAT-TEST-0001"))
                .andExpect(jsonPath("$.donnees.integre").value(true));

        mockMvc.perform(get("/api/neonatals/mere/" + patienteDuGyneco))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donnees.length()").value(1));
    }

    @Test
    @WithMockUser(username = "pediatre", roles = "PEDIATRE")
    @DisplayName("OS-06 : rattacher l'enfant a une autre mere casse la signature")
    void rerattachementDetecteParLaSignature() throws Exception {
        mockMvc.perform(post("/api/neonatals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsNeonatal(patienteDuGyneco)))
                .andExpect(status().isOk());

        var dossier = dossierNeonatalRepository.findAll().get(0);
        dossier.setMere(patienteRepository.findById(patienteDuConfrere).orElseThrow());
        dossierNeonatalRepository.save(dossier);

        mockMvc.perform(get("/api/neonatals/" + dossier.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donnees.integre").value(false));
    }

    // =========================================================================
    // US-09 : archivage
    // =========================================================================

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("US-09 : un dossier archive reste consultable")
    void dossierArchiveResteConsultable() throws Exception {
        Long id = creerDossierAvecRole(patienteDuGyneco);

        mockMvc.perform(put("/api/dossiers/" + id + "/archiver"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donnees.statut").value("ARCHIVE"));

        mockMvc.perform(get("/api/dossiers/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donnees.statut").value("ARCHIVE"));
    }

    // =========================================================================
    // OS-08 : tracabilite
    // =========================================================================

    @Test
    @WithMockUser(username = GYNECO, roles = "GYNECOLOGUE")
    @DisplayName("OS-08 : creation et consultation laissent chacune une trace")
    void chaqueOperationEstAuditee() throws Exception {
        Long id = creerDossier(patienteDuGyneco, "Diagnostic");
        mockMvc.perform(get("/api/dossiers/" + id)).andExpect(status().isOk());

        assertThat(actions()).contains("DOSSIER_CREATE", "DOSSIER_READ");

        AuditLog creation = auditLogRepository.findAll().stream()
                .filter(e -> "DOSSIER_CREATE".equals(e.getAction()))
                .findFirst().orElseThrow();
        assertThat(creation.getUtilisateur()).isEqualTo(GYNECO);
        assertThat(creation.getRole()).isEqualTo("GYNECOLOGUE");

        // Le journal ne doit contenir aucune donnee medicale : il est lisible
        // par l'administrateur, qui n'est pas soignant.
        assertThat(auditLogRepository.findAll())
                .noneMatch(e -> e.getDetails() != null && e.getDetails().contains("Diagnostic"));
    }

    // =========================================================================
    // Outils
    // =========================================================================

    private List<String> actions() {
        return auditLogRepository.findAll().stream().map(AuditLog::getAction).toList();
    }

    private String corpsDossier(Long patienteId, String diagnostic) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "patienteId", patienteId,
                "dateConsultation", LocalDate.now().toString(),
                "motif", "Consultation prenatale",
                "diagnostic", diagnostic,
                "traitement", "Fer + acide folique",
                "observations", "Tension 11/7"));
    }

    private String corpsNeonatal(Long mereId) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "mereId", mereId,
                "nomBebe", "Ousmane NDIAYE",
                "sexe", "M",
                "dateNaissance", LocalDate.now().toString(),
                "poidsGrammes", 3240,
                "tailleCm", 49,
                "scoreApgar", 9,
                "diagnostic", "Nouveau-ne a terme",
                "observations", "Allaitement maternel exclusif"));
    }

    /** Cree un dossier via l'API sous l'identite courante du test. */
    private Long creerDossier(Long patienteId, String diagnostic) throws Exception {
        mockMvc.perform(post("/api/dossiers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsDossier(patienteId, diagnostic)))
                .andExpect(status().isOk());
        return dossierMedicalRepository.findAll().get(0).getId();
    }

    /**
     * Cree un dossier en base sans passer par l'API : utilise par les tests
     * joues sous un role qui n'a pas le droit de rediger (ADMIN).
     */
    private Long creerDossierAvecRole(Long patienteId) {
        Patiente patiente = patienteRepository.findById(patienteId).orElseThrow();
        return dossierMedicalRepository.save(DossierMedical.builder()
                .numeroDossier("DM-TEST-0001")
                .patiente(patiente)
                .dateConsultation(LocalDate.now())
                .motif("Consultation prenatale")
                .diagnosticChiffre(protection.chiffrer("Diagnostic"))
                .statut("ACTIF")
                .creePar("gyneco.un")
                .build()).getId();
    }
}
