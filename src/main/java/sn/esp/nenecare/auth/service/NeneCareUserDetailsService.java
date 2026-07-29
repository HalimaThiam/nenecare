package sn.esp.nenecare.auth.service;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sn.esp.nenecare.user.model.User;
import sn.esp.nenecare.user.repository.UserRepository;

/**
 * Fait le pont entre la table "utilisateurs" et Spring Security.
 *
 * Sans ce bean, Spring Boot genere un compte "user" aleatoire au demarrage
 * et les comptes de la base ne servent a rien.
 *
 * Piege RBAC (voir README section 6) : hasRole('ADMIN') attend l'authority
 * "ROLE_ADMIN". Le prefixe est donc ajoute ici, une fois pour toutes.
 */
@Service
@RequiredArgsConstructor
public class NeneCareUserDetailsService implements UserDetailsService {

    public static final String PREFIXE_ROLE = "ROLE_";

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur introuvable : " + username));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getMotDePasse())
                .authorities(List.of(
                        new SimpleGrantedAuthority(PREFIXE_ROLE + user.getRole().name())))
                .disabled(!user.isActif())
                .build();
    }
}
