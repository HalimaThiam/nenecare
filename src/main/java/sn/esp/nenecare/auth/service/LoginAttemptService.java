package sn.esp.nenecare.auth.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

/**
 * Compte les tentatives de connexion echouees et bloque apres 5 echecs (US-04).
 *
 * Proprietaire : Elimane (auth). Squelette en memoire - a completer
 * (persistance / fenetre temporelle / notification administrateur).
 */
@Service
public class LoginAttemptService {

    private static final int MAX_TENTATIVES = 5;

    private final ConcurrentHashMap<String, AtomicInteger> tentatives = new ConcurrentHashMap<>();

    public void echecConnexion(String username) {
        tentatives.computeIfAbsent(username, k -> new AtomicInteger(0)).incrementAndGet();
        // TODO US-04 : si le seuil est atteint, generer une entree d'audit et notifier l'admin.
    }

    public void reinitialiser(String username) {
        tentatives.remove(username);
    }

    public boolean estBloque(String username) {
        AtomicInteger compteur = tentatives.get(username);
        return compteur != null && compteur.get() >= MAX_TENTATIVES;
    }
}
