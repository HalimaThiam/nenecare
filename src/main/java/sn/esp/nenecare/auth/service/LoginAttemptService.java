package sn.esp.nenecare.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * Compte les tentatives de connexion echouees et bloque apres 5 echecs (US-04).
 *
 * Proprietaire : Elimane (auth).
 *
 * Le blocage est TEMPOIRE : il expire au bout de {@value #DUREE_BLOCAGE_MINUTES}
 * minutes. Sans cette fenetre, cinq tentatives suffiraient a rendre un compte
 * definitivement inutilisable - un deni de service trivial contre le personnel
 * de la clinique.
 *
 * LIMITE CONNUE : compteurs en memoire, remis a zero au redemarrage et non
 * partages entre instances.
 */
@Service
public class LoginAttemptService {

    /** Nombre d'echecs tolere avant blocage (US-04). */
    public static final int MAX_TENTATIVES = 5;

    /** Duree du blocage, et duree au bout de laquelle les echecs sont oublies. */
    public static final long DUREE_BLOCAGE_MINUTES = 15;

    private final Map<String, Tentatives> parIdentifiant = new ConcurrentHashMap<>();

    /** Enregistre un echec. Renvoie true si le seuil vient d'etre atteint. */
    public boolean echecConnexion(String username) {
        Tentatives tentatives = parIdentifiant.compute(cle(username), (k, existant) -> {
            if (existant == null || existant.estPerimee()) {
                return new Tentatives(1, Instant.now());
            }
            return new Tentatives(existant.nombre() + 1, Instant.now());
        });
        return tentatives.nombre() == MAX_TENTATIVES;
    }

    /** Connexion reussie : le compteur repart de zero. */
    public void reinitialiser(String username) {
        parIdentifiant.remove(cle(username));
    }

    public boolean estBloque(String username) {
        Tentatives tentatives = parIdentifiant.get(cle(username));
        if (tentatives == null || tentatives.estPerimee()) {
            return false;
        }
        return tentatives.nombre() >= MAX_TENTATIVES;
    }

    /** Minutes restantes avant deblocage (au moins 1 tant que le blocage tient). */
    public long minutesRestantes(String username) {
        Tentatives tentatives = parIdentifiant.get(cle(username));
        if (tentatives == null || tentatives.estPerimee()) {
            return 0;
        }
        long ecoulees = Duration.between(tentatives.dernierEchec(), Instant.now()).toMinutes();
        return Math.max(1, DUREE_BLOCAGE_MINUTES - ecoulees);
    }

    /** Nombre d'echecs actuellement comptabilises (diagnostic / tests). */
    public int nombreEchecs(String username) {
        Tentatives tentatives = parIdentifiant.get(cle(username));
        return (tentatives == null || tentatives.estPerimee()) ? 0 : tentatives.nombre();
    }

    /**
     * Les identifiants sont compares sans tenir compte de la casse : sinon
     * "Admin", "ADMIN" et "admin" disposeraient chacun de 5 essais.
     */
    private String cle(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private record Tentatives(int nombre, Instant dernierEchec) {
        boolean estPerimee() {
            return dernierEchec.isBefore(
                    Instant.now().minus(Duration.ofMinutes(DUREE_BLOCAGE_MINUTES)));
        }
    }
}
