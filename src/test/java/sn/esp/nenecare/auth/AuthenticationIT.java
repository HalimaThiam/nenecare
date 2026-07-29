package sn.esp.nenecare.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import sn.esp.nenecare.auth.service.LoginAttemptService;
import sn.esp.nenecare.user.model.Role;
import sn.esp.nenecare.user.model.User;
import sn.esp.nenecare.user.repository.UserRepository;

/**
 * Parcours d'authentification de bout en bout (US-01, US-02, US-03, US-04).
 *
 * Volontairement SANS @Transactional : le service d'audit ecrit dans une
 * transaction separee (REQUIRES_NEW) et ses entrees ne seraient pas visibles
 * de la meme facon qu'en production. Le nettoyage se fait donc explicitement
 * avant chaque test.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationIT {

    private static final String MOT_DE_PASSE = "MotDePasseTest2026!";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private LoginAttemptService loginAttemptService;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void preparerJeuDeDonnees() {
        userRepository.deleteAll();
        auditLogRepository.deleteAll();

        userRepository.save(User.builder()
                .username("gyneco.test")
                .motDePasse(passwordEncoder.encode(MOT_DE_PASSE))
                .nomComplet("Amadou DIAO")
                .role(Role.GYNECOLOGUE)
                .actif(true)
                .build());

        userRepository.save(User.builder()
                .username("compte.revoque")
                .motDePasse(passwordEncoder.encode(MOT_DE_PASSE))
                .nomComplet("Compte Revoque")
                .role(Role.INFIRMIER)
                .actif(false)
                .build());

        // Les compteurs de blocage vivent en memoire : ils survivraient d'un
        // test a l'autre sans cette remise a zero.
        loginAttemptService.reinitialiser("gyneco.test");
        loginAttemptService.reinitialiser("inconnu");
        loginAttemptService.reinitialiser("cible.bruteforce");
    }

    private String corpsLogin(String username, String motDePasse) throws Exception {
        return objectMapper.writeValueAsString(
                java.util.Map.of("username", username, "motDePasse", motDePasse));
    }

    // =========================================================================
    // US-01 : connexion
    // =========================================================================

    @Test
    @DisplayName("US-01 : des identifiants valides renvoient un jeton et le role")
    void loginReussi() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsLogin("gyneco.test", MOT_DE_PASSE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.succes").value(true))
                .andExpect(jsonPath("$.donnees.token").isNotEmpty())
                .andExpect(jsonPath("$.donnees.role").value("GYNECOLOGUE"))
                .andExpect(jsonPath("$.donnees.nomComplet").value("Amadou DIAO"))
                // Le mot de passe ne doit jamais figurer dans la reponse.
                .andExpect(jsonPath("$.donnees.motDePasse").doesNotExist());
    }

    @Test
    @DisplayName("Un mot de passe faux renvoie 401, pas 400")
    void motDePasseIncorrect() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsLogin("gyneco.test", "mauvais-mot-de-passe")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Compte inconnu et mot de passe faux donnent le meme message")
    void pasDEnumerationDeComptes() throws Exception {
        String messageCompteInconnu = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsLogin("inconnu", MOT_DE_PASSE)))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String messageMauvaisMdp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsLogin("gyneco.test", "faux")))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(extraireMessage(messageCompteInconnu))
                .isEqualTo(extraireMessage(messageMauvaisMdp));
    }

    @Test
    @DisplayName("US-19 : un compte desactive ne peut plus se connecter")
    void compteDesactive() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsLogin("compte.revoque", MOT_DE_PASSE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Une requete sans identifiant renvoie 400, pas 500")
    void validationDuCorps() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsLogin("", "")))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // US-04 : blocage apres 5 echecs
    // =========================================================================

    @Test
    @DisplayName("US-04 : le compte est bloque au 6e essai, puis debloque apres la fenetre")
    void blocageApresCinqEchecs() throws Exception {
        for (int i = 0; i < LoginAttemptService.MAX_TENTATIVES; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpsLogin("gyneco.test", "faux")))
                    .andExpect(status().isUnauthorized());
        }

        // 6e tentative : meme avec le BON mot de passe, l'acces est refuse.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsLogin("gyneco.test", MOT_DE_PASSE)))
                .andExpect(status().isLocked());

        // Le blocage est temporaire : il doit annoncer une duree, sinon le
        // compte serait mort jusqu'au redemarrage du serveur.
        assertThat(loginAttemptService.minutesRestantes("gyneco.test")).isPositive();

        // Apres deblocage (simule par la reinitialisation), l'acces revient.
        loginAttemptService.reinitialiser("gyneco.test");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsLogin("gyneco.test", MOT_DE_PASSE)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("US-04 : un identifiant inexistant est bloque aussi (pas d'enumeration)")
    void blocageSurIdentifiantInconnu() throws Exception {
        for (int i = 0; i < LoginAttemptService.MAX_TENTATIVES; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpsLogin("cible.bruteforce", "faux")))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsLogin("cible.bruteforce", "faux")))
                .andExpect(status().isLocked());
    }

    // =========================================================================
    // US-03 : deconnexion et revocation
    // =========================================================================

    @Test
    @DisplayName("US-03 : apres deconnexion, l'ancien jeton n'ouvre plus rien")
    void revocationDuJetonALaDeconnexion() throws Exception {
        userRepository.save(User.builder()
                .username("admin.test")
                .motDePasse(passwordEncoder.encode(MOT_DE_PASSE))
                .nomComplet("Halima THIAM")
                .role(Role.ADMIN)
                .actif(true)
                .build());

        String jeton = extraireJeton(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsLogin("admin.test", MOT_DE_PASSE)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        // Le jeton fonctionne...
        mockMvc.perform(get("/api/audit/logs").header("Authorization", "Bearer " + jeton))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + jeton))
                .andExpect(status().isOk());

        // ...et ne fonctionne plus apres la deconnexion.
        mockMvc.perform(get("/api/audit/logs").header("Authorization", "Bearer " + jeton))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // Journalisation (OS-08)
    // =========================================================================

    @Test
    @DisplayName("OS-08 : reussites et echecs de connexion sont journalises")
    void connexionsJournalisees() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpsLogin("gyneco.test", MOT_DE_PASSE)));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpsLogin("gyneco.test", "faux")));

        assertThat(auditLogRepository.findByAction("LOGIN_SUCCESS")).hasSize(1);
        assertThat(auditLogRepository.findByAction("LOGIN_FAILURE")).hasSize(1);
    }

    // -------------------------------------------------------------------------

    private String extraireJeton(String reponseJson) throws Exception {
        return objectMapper.readTree(reponseJson).path("donnees").path("token").asText();
    }

    private String extraireMessage(String reponseJson) throws Exception {
        return objectMapper.readTree(reponseJson).path("message").asText();
    }
}
