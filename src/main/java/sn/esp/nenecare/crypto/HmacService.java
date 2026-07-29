package sn.esp.nenecare.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

/**
 * Signature et verification d'integrite HMAC-SHA256 (OS-07, OS-08).
 * Le fournisseur Bouncy Castle est enregistre par CryptoConfig au demarrage.
 *
 * Proprietaire : Amadou (crypto).
 */
@Service
public class HmacService {

    private static final String ALGORITHM = "HmacSHA256";

    /** Genere une signature HMAC d'une donnee. */
    public String sign(String data, String secretKey) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(
            Base64.getDecoder().decode(secretKey), ALGORITHM);
        Mac mac = Mac.getInstance(ALGORITHM, "BC");
        mac.init(keySpec);
        // Encodage explicite : sans cela la signature dependrait du charset par
        // defaut de la machine et ne serait pas reproductible d'un poste a l'autre.
        byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature);
    }

    /**
     * Verifie l'integrite : recalcule la signature et la compare.
     *
     * La comparaison passe par MessageDigest.isEqual, qui parcourt toujours
     * l'ensemble des octets. Un equals() classique s'arrete au premier octet
     * different : le temps de reponse permettrait alors de reconstituer une
     * signature valide octet par octet.
     */
    public boolean verify(String data, String secretKey, String expectedSignature) throws Exception {
        if (expectedSignature == null) {
            return false;
        }
        String actualSignature = sign(data, secretKey);
        return MessageDigest.isEqual(
            actualSignature.getBytes(StandardCharsets.UTF_8),
            expectedSignature.getBytes(StandardCharsets.UTF_8));
    }

    /** Genere une cle HMAC aleatoire (256 bits, Base64). */
    public String generateKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
