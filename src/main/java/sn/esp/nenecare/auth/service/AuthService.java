package sn.esp.nenecare.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sn.esp.nenecare.auth.dto.LoginRequest;
import sn.esp.nenecare.auth.dto.LoginResponse;
import sn.esp.nenecare.auth.jwt.JwtService;
import sn.esp.nenecare.user.model.User;
import sn.esp.nenecare.user.repository.UserRepository;

/**
 * Logique d'authentification (US-01, US-03, US-04).
 *
 * Proprietaire : Elimane (auth). Squelette de demarrage : la verification
 * bcrypt et la generation du jeton sont cablees ; reste a brancher l'audit,
 * la revocation (logout) et le blocage effectif.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    public LoginResponse login(LoginRequest requete) {
        if (loginAttemptService.estBloque(requete.getUsername())) {
            throw new IllegalArgumentException(
                "Compte temporairement bloque suite a trop de tentatives echouees.");
        }

        User user = userRepository.findByUsername(requete.getUsername())
                .filter(User::isActif)
                .orElse(null);

        boolean motDePasseOk = user != null
                && passwordEncoder.matches(requete.getMotDePasse(), user.getMotDePasse());

        if (!motDePasseOk) {
            loginAttemptService.echecConnexion(requete.getUsername());
            // TODO : enregistrer l'echec dans l'audit (succes=false).
            throw new IllegalArgumentException("Identifiant ou mot de passe incorrect.");
        }

        loginAttemptService.reinitialiser(requete.getUsername());
        String token = jwtService.genererToken(user.getUsername(), user.getRole().name());
        // TODO : enregistrer la connexion reussie dans l'audit.
        return new LoginResponse(token, user.getUsername(),
                user.getRole().name(), jwtService.getExpirationMs());
    }

    public void logout(String token) {
        // TODO US-03 : ajouter le jeton (ou son jti) a une liste de revocation.
    }
}
