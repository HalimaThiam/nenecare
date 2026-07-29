package sn.esp.nenecare.auth.service;

import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sn.esp.nenecare.audit.service.AuditService;
import sn.esp.nenecare.auth.dto.LoginRequest;
import sn.esp.nenecare.auth.dto.LoginResponse;
import sn.esp.nenecare.auth.jwt.JetonRevoqueService;
import sn.esp.nenecare.auth.jwt.JwtService;
import sn.esp.nenecare.common.exception.CompteBloqueException;
import sn.esp.nenecare.common.exception.IdentifiantsInvalidesException;
import sn.esp.nenecare.user.model.User;
import sn.esp.nenecare.user.repository.UserRepository;

/**
 * Logique d'authentification (US-01, US-03, US-04).
 *
 * Proprietaire : Elimane (auth).
 *
 * Toute tentative - reussie ou non - laisse une trace dans le journal d'audit
 * (OS-08) : c'est la seule facon de detecter une attaque par force brute apres
 * coup.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String ROLE_INCONNU = "INCONNU";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    private final JetonRevoqueService jetonRevoqueService;
    private final AuditService auditService;

    public LoginResponse login(LoginRequest requete, String adresseIp) {
        String username = requete.getUsername();

        if (loginAttemptService.estBloque(username)) {
            auditService.logAction(username, ROLE_INCONNU, "LOGIN_BLOCKED", null,
                    "Tentative sur un identifiant bloque", adresseIp, false);
            throw new CompteBloqueException(loginAttemptService.minutesRestantes(username));
        }

        User user = userRepository.findByUsername(username)
                .filter(User::isActif)
                .orElse(null);

        boolean motDePasseOk;
        if (user == null) {
            // Un bcrypt est quand meme calcule, contre un hachage factice : sans
            // cela un compte inexistant repondrait bien plus vite qu'un compte
            // existant, ce qui suffit a enumerer le personnel de la clinique.
            passwordEncoder.matches(requete.getMotDePasse(), hachageFactice());
            motDePasseOk = false;
        } else {
            motDePasseOk = passwordEncoder.matches(
                    requete.getMotDePasse(), user.getMotDePasse());
        }

        if (!motDePasseOk) {
            boolean seuilAtteint = loginAttemptService.echecConnexion(username);
            auditService.logAction(username,
                    user != null ? user.getRole().name() : ROLE_INCONNU,
                    "LOGIN_FAILURE", null,
                    seuilAtteint
                        ? "Seuil de " + LoginAttemptService.MAX_TENTATIVES
                          + " echecs atteint : identifiant bloque"
                        : "Echec d'authentification",
                    adresseIp, false);
            throw new IdentifiantsInvalidesException();
        }

        loginAttemptService.reinitialiser(username);
        String token = jwtService.genererToken(user.getUsername(), user.getRole().name());

        auditService.logAction(user.getUsername(), user.getRole().name(),
                "LOGIN_SUCCESS", null, "Connexion reussie", adresseIp, true);

        return new LoginResponse(token, user.getUsername(), user.getNomComplet(),
                user.getRole().name(), jwtService.getExpirationMs());
    }

    /**
     * Hachage bcrypt d'une valeur aleatoire, calcule une seule fois au premier
     * besoin. Sert uniquement de leurre pour egaliser les temps de reponse.
     */
    private volatile String hachageFactice;

    private String hachageFactice() {
        if (hachageFactice == null) {
            synchronized (this) {
                if (hachageFactice == null) {
                    hachageFactice = passwordEncoder.encode(
                            java.util.UUID.randomUUID().toString());
                }
            }
        }
        return hachageFactice;
    }

    /** US-03 : le jeton est revoque jusqu'a son expiration naturelle. */
    public void logout(String token, String adresseIp) {
        if (token == null || !jwtService.estValide(token)) {
            return;
        }
        String username = jwtService.extraireUsername(token);
        String role = jwtService.extraireRole(token);

        jetonRevoqueService.revoquer(
                jwtService.extraireJti(token),
                Instant.ofEpochMilli(jwtService.extraireExpiration(token).getTime()));

        auditService.logAction(username, role, "LOGOUT", null,
                "Deconnexion et revocation du jeton", adresseIp, true);
    }
}
