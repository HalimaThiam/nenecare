package sn.esp.nenecare.audit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import sn.esp.nenecare.audit.repository.AuditLogRepository;
import sn.esp.nenecare.user.model.Role;
import sn.esp.nenecare.user.model.User;
import sn.esp.nenecare.user.repository.UserRepository;

/**
 * Controle d'acces au journal d'audit (OS-05, US-16).
 *
 * Le journal revele qui consulte quel dossier : il doit etre inaccessible
 * sans authentification ET inaccessible aux roles non administrateurs.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuditRbacIT {

    private static final String MOT_DE_PASSE = "MotDePasseTest2026!";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void preparerJeuDeDonnees() {
        userRepository.deleteAll();
        auditLogRepository.deleteAll();

        userRepository.save(compte("admin.audit", Role.ADMIN));
        userRepository.save(compte("sagefemme.audit", Role.SAGE_FEMME));
    }

    private User compte(String username, Role role) {
        return User.builder()
                .username(username)
                .motDePasse(passwordEncoder.encode(MOT_DE_PASSE))
                .nomComplet("Compte " + role.name())
                .role(role)
                .actif(true)
                .build();
    }

    @Test
    @DisplayName("Sans jeton, le journal d'audit renvoie 401")
    void journalInaccessibleSansJeton() throws Exception {
        mockMvc.perform(get("/api/audit/logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Avec un jeton invente, le journal renvoie 401")
    void journalInaccessibleAvecJetonInvalide() throws Exception {
        mockMvc.perform(get("/api/audit/logs")
                        .header("Authorization", "Bearer jeton.completement.invente"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("OS-05 : une sage-femme authentifiee recoit 403 sur le journal")
    void journalInterditAuxNonAdmins() throws Exception {
        mockMvc.perform(get("/api/audit/logs")
                        .header("Authorization", "Bearer " + jetonDe("sagefemme.audit")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("US-16 : l'administrateur consulte le journal")
    void journalAccessibleALAdmin() throws Exception {
        mockMvc.perform(get("/api/audit/logs")
                        .header("Authorization", "Bearer " + jetonDe("admin.audit")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Le journal est en lecture seule : aucune API n'y ajoute d'entree")
    void aucunEndpointDEcritureSurLeJournal() throws Exception {
        // L'ancien POST /api/audit/test permettait a n'importe qui de fabriquer
        // une entree signee au nom d'un autre utilisateur. Il ne doit plus exister.
        mockMvc.perform(post("/api/audit/test")
                        .param("utilisateur", "faux.medecin")
                        .param("role", "ADMIN")
                        .param("action", "DOSSIER_DELETE")
                        .header("Authorization", "Bearer " + jetonDe("admin.audit")))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------

    private String jetonDe(String username) throws Exception {
        String reponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("username", username,
                                                 "motDePasse", MOT_DE_PASSE))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(reponse).path("donnees").path("token").asText();
    }
}
