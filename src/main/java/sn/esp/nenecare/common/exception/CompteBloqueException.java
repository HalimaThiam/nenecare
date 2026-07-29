package sn.esp.nenecare.common.exception;

/**
 * Trop de tentatives de connexion echouees (US-04).
 *
 * Le blocage porte sur l'identifiant saisi, meme s'il ne correspond a aucun
 * compte : le message ne revele donc pas l'existence d'un compte.
 */
public class CompteBloqueException extends RuntimeException {

    public CompteBloqueException(long minutesRestantes) {
        super("Trop de tentatives echouees. Nouvel essai possible dans "
              + minutesRestantes + " minute(s).");
    }
}
