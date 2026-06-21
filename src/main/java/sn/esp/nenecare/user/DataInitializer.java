package sn.esp.nenecare.user;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sn.esp.nenecare.user.model.Role;
import sn.esp.nenecare.user.model.User;
import sn.esp.nenecare.user.repository.UserRepository;

/**
 * Cree des comptes de test au demarrage si la base est vide.
 * Permet de tester l'authentification sans saisie manuelle en base.
 *
 * Idempotent : ne recree pas un compte existant (existsByUsername).
 * Les mots de passe sont haches bcrypt avant persistance (OS-12).
 *
 * Proprietaire : Elimane (auth / user).
 * NOTE : a desactiver ou retirer avant une mise en production reelle.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        creerSiAbsent("admin", "Admin Systeme", Role.ADMIN, "Admin@123");
        creerSiAbsent("gyneco1", "Dr Aissatou Gynecologue", Role.GYNECOLOGUE, "Gyneco@123");
        creerSiAbsent("pediatre1", "Dr Moussa Pediatre", Role.PEDIATRE, "Pediatre@123");
        creerSiAbsent("sage1", "Awa Sage-Femme", Role.SAGE_FEMME, "Sage@123");
        creerSiAbsent("infirmier1", "Fatou Infirmiere", Role.INFIRMIER, "Infirmier@123");
        creerSiAbsent("secretaire1", "Ndeye Secretaire", Role.SECRETAIRE, "Secretaire@123");
    }

    private void creerSiAbsent(String username, String nomComplet, Role role, String motDePasseClair) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        User user = User.builder()
                .username(username)
                .motDePasse(passwordEncoder.encode(motDePasseClair))
                .nomComplet(nomComplet)
                .role(role)
                .actif(true)
                .creeLe(LocalDateTime.now())
                .build();
        userRepository.save(user);
        log.info("Compte de test cree : {} ({})", username, role);
    }
}
