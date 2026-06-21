package sn.esp.nenecare.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sn.esp.nenecare.audit.service.AuditService;
import sn.esp.nenecare.auth.dto.LoginRequest;
import sn.esp.nenecare.auth.dto.LoginResponse;
import sn.esp.nenecare.auth.jwt.JwtService;
import sn.esp.nenecare.user.model.User;
import sn.esp.nenecare.user.repository.UserRepository;

/**
 * Logique d'authentification (US-01, US-03, US-04).
 *
 * Proprietaire : Elimane (auth).
 * Reutilise : PasswordEncoder bcrypt (SecurityConfig), JwtService (30 min),
 * AuditService (tracabilite des connexions).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    private final AuditService auditService;

    /**
     * Authentifie un utilisateur et renvoie un JWT (US-01).
     * @param adresseIp adresse IP de l'appelant, pour la tracabilite d'audit.
     */
    public LoginResponse login(LoginRequest requete, String adresseIp) {
        String username = requete.getUsername();

        if (loginAttemptService.estBloque(username)) {
            throw new IllegalArgumentException(
                "Compte temporairement bloque suite a trop de tentatives echouees.");
        }

        User user = userRepository.findByUsername(username)
                .filter(User::isActif)
                .orElse(null);

        boolean motDePasseOk = user != null
                && passwordEncoder.matches(requete.getMotDePasse(), user.getMotDePasse());

        if (!motDePasseOk) {
            int nbEchecs = loginAttemptService.echecConnexion(username);
            auditService.logAction(username, "INCONNU", "LOGIN_FAILED", null,
                    "Echec de connexion (" + nbEchecs + "/" + LoginAttemptService.MAX_TENTATIVES + ")",
                    adresseIp, false);
            if (nbEchecs >= LoginAttemptService.MAX_TENTATIVES) {
                auditService.logAction(username, "INCONNU", "COMPTE_BLOQUE", null,
                        "Compte bloque apres " + nbEchecs + " tentatives echouees",
                        adresseIp, false);
            }
            throw new IllegalArgumentException("Identifiant ou mot de passe incorrect.");
        }

        loginAttemptService.reinitialiser(username);
        String token = jwtService.genererToken(user.getUsername(), user.getRole().name());

        auditService.logAction(user.getUsername(), user.getRole().name(),
                "LOGIN", null, "Connexion reussie", adresseIp, true);

        return new LoginResponse(token, user.getUsername(),
                user.getRole().name(), jwtService.getExpirationMs());
    }

    public void logout(String token) {
        // TODO US-03 : ajouter le jeton a une liste de revocation.
    }
}
