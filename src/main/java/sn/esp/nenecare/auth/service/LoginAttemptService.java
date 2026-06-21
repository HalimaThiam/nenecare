package sn.esp.nenecare.auth.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

/**
 * Compte les tentatives de connexion echouees et bloque apres 5 echecs (US-04).
 *
 * Proprietaire : Elimane (auth). Stockage en memoire (suffisant pour un serveur
 * mono-instance ; a externaliser si l'app est repliquee).
 */
@Service
public class LoginAttemptService {

    public static final int MAX_TENTATIVES = 5;

    private final ConcurrentHashMap<String, AtomicInteger> tentatives = new ConcurrentHashMap<>();

    /** Incremente le compteur d'echecs et renvoie le nouveau total. */
    public int echecConnexion(String username) {
        return tentatives.computeIfAbsent(username, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /** Remet le compteur a zero (connexion reussie). */
    public void reinitialiser(String username) {
        tentatives.remove(username);
    }

    public boolean estBloque(String username) {
        AtomicInteger compteur = tentatives.get(username);
        return compteur != null && compteur.get() >= MAX_TENTATIVES;
    }
}
