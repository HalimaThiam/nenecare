package sn.esp.nenecare.patient.model;

/**
 * Statuts possibles d'un dossier (US-09).
 *
 * Volontairement des constantes de chaine et non une enum : le statut est
 * ecrit tel quel en base et lu par des requetes SQL d'audit ; une valeur
 * lisible ("ARCHIVE") vaut mieux qu'un ordinal a interpreter.
 */
public final class StatutDossier {

    /** Dossier en cours de suivi. */
    public static final String ACTIF = "ACTIF";

    /**
     * Dossier ferme. Reste consultable et conserve sa signature : un dossier
     * medical ne se supprime pas, il s'archive (obligation de conservation).
     */
    public static final String ARCHIVE = "ARCHIVE";

    private StatutDossier() {
        // Classe utilitaire.
    }
}
