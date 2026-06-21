package sn.esp.nenecare.crypto;

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
        byte[] signature = mac.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(signature);
    }

    /** Verifie l'integrite : recalcule la signature et la compare. */
    public boolean verify(String data, String secretKey, String expectedSignature) throws Exception {
        String actualSignature = sign(data, secretKey);
        return actualSignature.equals(expectedSignature);
    }

    /** Genere une cle HMAC aleatoire (256 bits, Base64). */
    public String generateKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
