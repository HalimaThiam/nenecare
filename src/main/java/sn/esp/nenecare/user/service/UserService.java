package sn.esp.nenecare.user.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sn.esp.nenecare.user.model.User;
import sn.esp.nenecare.user.repository.UserRepository;

/**
 * Gestion des comptes utilisateurs.
 * STUB de base - a completer par Elimane (auth) / Halima (gestion comptes US-18/19).
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // TODO US-18 : creer / modifier / desactiver un compte.
    // TODO US-19 : revoquer immediatement tous les acces d'un utilisateur.
}
