package sn.esp.nenecare.common.exception;

/**
 * Identifiant inconnu, mot de passe incorrect ou compte desactive.
 *
 * Un seul et meme message pour ces trois cas : distinguer "utilisateur
 * inconnu" de "mot de passe faux" permettrait d'enumerer les comptes
 * existants de la clinique.
 */
public class IdentifiantsInvalidesException extends RuntimeException {

    public IdentifiantsInvalidesException() {
        super("Identifiant ou mot de passe incorrect.");
    }
}
