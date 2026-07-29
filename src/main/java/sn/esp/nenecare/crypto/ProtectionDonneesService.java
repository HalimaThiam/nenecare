package sn.esp.nenecare.crypto;

import java.util.Base64;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Protection des donnees medicales au repos : chiffrement AES-256-GCM (OS-03)
 * et signature d'integrite HMAC-SHA256 (OS-07).
 *
 * Proprietaire : Amadou (crypto).
 *
 * Ce service porte les CLES ; AesGcmService et HmacService ne portent que les
 * algorithmes. Les services metier n'ont ainsi jamais a manipuler de cle :
 * ils appellent chiffrer/dechiffrer/signer et rien d'autre. Une cle qui ne
 * circule pas est une cle qu'on n'oublie pas dans un log.
 *
 * Les cles sont validees AU DEMARRAGE : une cle absente ou de mauvaise taille
 * fait echouer le lancement de l'application. C'est volontaire - une cle
 * silencieusement invalide produirait des dossiers illisibles, decouverts
 * seulement le jour ou un medecin en a besoin.
 */
@Service
public class ProtectionDonneesService {

    /** AES-256 : la cle fait exactement 32 octets, ni plus ni moins. */
    private static final int TAILLE_CLE_AES = 32;

    /** HMAC-SHA256 : une cle plus courte que l'empreinte affaiblit la signature. */
    private static final int TAILLE_MIN_CLE_HMAC = 32;

    private final AesGcmService aesGcmService;
    private final HmacService hmacService;

    private final SecretKey cleAes;
    private final String cleHmacBase64;

    public ProtectionDonneesService(AesGcmService aesGcmService,
                                    HmacService hmacService,
                                    @Value("${nenecare.crypto.aes-key}") String cleAesBase64,
                                    @Value("${nenecare.crypto.hmac-key}") String cleHmacBase64) {

        this.aesGcmService = aesGcmService;
        this.hmacService = hmacService;

        this.cleAes = aesGcmService.keyFromBase64(
                valider(cleAesBase64, "nenecare.crypto.aes-key", TAILLE_CLE_AES, true));
        this.cleHmacBase64 =
                valider(cleHmacBase64, "nenecare.crypto.hmac-key", TAILLE_MIN_CLE_HMAC, false);
    }

    // =========================================================================
    // Chiffrement des champs sensibles (OS-03)
    // =========================================================================

    /**
     * Chiffre une valeur avant enregistrement en base.
     *
     * AES-GCM tire un IV aleatoire a chaque appel : deux chiffrements du meme
     * texte donnent deux resultats differents. Impossible donc de deduire, en
     * comparant deux lignes de la table, que deux patientes ont le meme
     * diagnostic.
     *
     * @return null si l'entree est null (champ medical non renseigne)
     */
    public String chiffrer(String clair) {
        if (clair == null) {
            return null;
        }
        try {
            return aesGcmService.encrypt(clair, cleAes);
        } catch (Exception e) {
            throw new IllegalStateException("Echec du chiffrement d'une donnee medicale", e);
        }
    }

    /**
     * Dechiffre une valeur relue en base.
     *
     * GCM est un mode authentifie : si l'octet stocke a ete modifie en base,
     * le dechiffrement echoue au lieu de rendre un texte errone.
     */
    public String dechiffrer(String chiffre) {
        if (chiffre == null) {
            return null;
        }
        try {
            return aesGcmService.decrypt(chiffre, cleAes);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Echec du dechiffrement : donnee alteree ou cle incorrecte", e);
        }
    }

    // =========================================================================
    // Integrite (OS-07)
    // =========================================================================

    /** Signature HMAC-SHA256 du contenu metier d'un dossier. */
    public String signer(String message) {
        try {
            return hmacService.sign(message, cleHmacBase64);
        } catch (Exception e) {
            throw new IllegalStateException("Echec du calcul de la signature d'integrite", e);
        }
    }

    /** Verifie qu'un dossier n'a pas ete modifie hors de l'application. */
    public boolean verifier(String message, String signatureAttendue) {
        try {
            return hmacService.verify(message, cleHmacBase64, signatureAttendue);
        } catch (Exception e) {
            return false;
        }
    }

    // =========================================================================
    // Validation au demarrage
    // =========================================================================

    private String valider(String cleBase64, String propriete, int tailleOctets, boolean exacte) {
        if (cleBase64 == null || cleBase64.isBlank()) {
            throw new IllegalStateException(
                    "La propriete " + propriete + " est obligatoire (cle Base64).");
        }

        byte[] octets;
        try {
            octets = Base64.getDecoder().decode(cleBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "La propriete " + propriete + " doit etre encodee en Base64. "
                    + "Generer une cle : openssl rand -base64 32", e);
        }

        boolean tailleOk = exacte ? octets.length == tailleOctets : octets.length >= tailleOctets;
        if (!tailleOk) {
            throw new IllegalStateException(
                    "La cle " + propriete + " fait " + octets.length + " octets, "
                    + (exacte ? "il en faut exactement " : "il en faut au moins ") + tailleOctets
                    + ". Generer une cle : openssl rand -base64 " + tailleOctets);
        }

        return cleBase64.trim();
    }
}
