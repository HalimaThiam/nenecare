package sn.esp.nenecare.auth.jwt;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * Liste de revocation des jetons JWT (US-03 : deconnexion explicite).
 *
 * Un JWT est valide jusqu'a son expiration ; pour le revoquer avant terme
 * (logout), on l'ajoute ici. Stockage en memoire (mono-instance).
 *
 * Proprietaire : Elimane (auth).
 * NOTE coordination Halima : le JwtAuthFilter doit appeler estRevoque(token)
 * et rejeter la requete si le jeton est present dans cette liste.
 */
@Service
public class TokenBlacklistService {

    private final Set<String> jetonsRevoques = ConcurrentHashMap.newKeySet();

    public void revoquer(String token) {
        jetonsRevoques.add(token);
    }

    public boolean estRevoque(String token) {
        return jetonsRevoques.contains(token);
    }
}
