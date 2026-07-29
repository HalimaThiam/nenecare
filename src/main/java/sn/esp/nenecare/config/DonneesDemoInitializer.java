package sn.esp.nenecare.config;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import sn.esp.nenecare.user.model.Role;
import sn.esp.nenecare.user.model.User;
import sn.esp.nenecare.user.repository.UserRepository;

/**
 * Cree un compte par role au demarrage, pour que l'application soit
 * immediatement testable (demo, recette, developpement frontend).
 *
 * ACTIF UNIQUEMENT si nenecare.demo.enabled=true - c'est le cas dans le profil
 * "dev" et jamais dans la configuration par defaut. Les comptes ne sont crees
 * que si la table est vide, pour ne pas ecraser de vrais comptes.
 *
 * A NE JAMAIS activer en production : les mots de passe sont connus.
 */
@Configuration
@ConditionalOnProperty(name = "nenecare.demo.enabled", havingValue = "true")
public class DonneesDemoInitializer {

    private static final Logger log = LoggerFactory.getLogger(DonneesDemoInitializer.class);

    @Bean
    public ApplicationRunner creerComptesDemo(UserRepository userRepository,
                                              PasswordEncoder passwordEncoder,
                                              @Value("${nenecare.demo.mot-de-passe}") String motDePasse) {
        return args -> {
            if (userRepository.count() > 0) {
                log.info("Comptes de demonstration ignores : la table utilisateurs n'est pas vide.");
                return;
            }

            List<User> comptes = List.of(
                compte("admin",       "Halima THIAM",   Role.ADMIN,       motDePasse, passwordEncoder),
                compte("gyneco",      "Amadou DIAO",    Role.GYNECOLOGUE, motDePasse, passwordEncoder),
                compte("pediatre",    "Elimane KA",     Role.PEDIATRE,    motDePasse, passwordEncoder),
                compte("sagefemme",   "Hadja DIALLO",   Role.SAGE_FEMME,  motDePasse, passwordEncoder),
                compte("infirmier",   "Moussa NDIAYE",  Role.INFIRMIER,   motDePasse, passwordEncoder),
                compte("secretaire",  "Awa FALL",       Role.SECRETAIRE,  motDePasse, passwordEncoder)
            );

            userRepository.saveAll(comptes);

            log.warn("=================================================================");
            log.warn(" COMPTES DE DEMONSTRATION CREES (profil de developpement)");
            comptes.forEach(user ->
                log.warn("   {} / {}   role={}", user.getUsername(), motDePasse, user.getRole()));
            log.warn(" Ces comptes ne doivent JAMAIS exister en production.");
            log.warn("=================================================================");
        };
    }

    private User compte(String username, String nomComplet, Role role,
                        String motDePasse, PasswordEncoder encoder) {
        return User.builder()
                .username(username)
                .motDePasse(encoder.encode(motDePasse))
                .nomComplet(nomComplet)
                .role(role)
                .actif(true)
                .creeLe(LocalDateTime.now())
                .build();
    }
}
