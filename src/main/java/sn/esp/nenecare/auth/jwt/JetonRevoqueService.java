package sn.esp.nenecare.auth.jwt;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * Liste de revocation des jetons JWT (US-03 : deconnexion effective).
 *
 * Un JWT est autoportant : le serveur ne peut pas "l'oublier". A la
 * deconnexion, on memorise donc son identifiant unique (jti) jusqu'a sa date
 * d'expiration naturelle ; passe cette date, l'entree est inutile et purgee.
 *
 * LIMITE CONNUE : stockage en memoire, donc perdu au redemarrage et non
 * partage entre instances. Suffisant pour un deploiement mono-instance ;
 * a remplacer par Redis ou une table dediee pour une mise en production.
 */
@Service
public class JetonRevoqueService {

    /** jti -> date d'expiration du jeton. */
    private final Map<String, Instant> revoques = new ConcurrentHashMap<>();

    /** Revoque un jeton jusqu'a son expiration naturelle. */
    public void revoquer(String jti, Instant expiration) {
        if (jti == null || expiration == null) {
            return;
        }
        purger();
        revoques.put(jti, expiration);
    }

    public boolean estRevoque(String jti) {
        if (jti == null) {
            return false;
        }
        Instant expiration = revoques.get(jti);
        if (expiration == null) {
            return false;
        }
        if (expiration.isBefore(Instant.now())) {
            revoques.remove(jti);
            return false;
        }
        return true;
    }

    /** Retire les jetons deja expires : la liste ne grossit pas indefiniment. */
    private void purger() {
        Instant maintenant = Instant.now();
        revoques.entrySet().removeIf(entree -> entree.getValue().isBefore(maintenant));
    }

    /** Nombre de jetons actuellement revoques (diagnostic / tests). */
    public int taille() {
        purger();
        return revoques.size();
    }
}
