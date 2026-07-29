package sn.esp.nenecare.common.exception;

/**
 * Ressource metier demandee mais inexistante : renvoyee en 404.
 *
 * Le message ne dit jamais si l'identifiant existe et appartient a quelqu'un
 * d'autre, ou s'il n'existe pas du tout : la difference permettrait de
 * denombrer les patientes de la clinique en essayant les identifiants un a un.
 */
public class RessourceIntrouvableException extends RuntimeException {

    public RessourceIntrouvableException(String message) {
        super(message);
    }

    public static RessourceIntrouvableException patiente(Long id) {
        return new RessourceIntrouvableException("Patiente introuvable (identifiant " + id + ").");
    }

    public static RessourceIntrouvableException dossier(Long id) {
        return new RessourceIntrouvableException("Dossier introuvable (identifiant " + id + ").");
    }
}
