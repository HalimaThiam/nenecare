package sn.esp.nenecare.encryption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Chiffrement AES-256-GCM authentifié.
 *
 * Format stocké en base : Base64( IV[12] || ChiffrTxt[N] || Tag[16] )
 * Le tag GCM est automatiquement annexé au chiffré par le JCE.
 *
 * Chaque appel à encrypt() génère un IV aléatoire distinct — essentiel en GCM
 * pour éviter la réutilisation IV+clé qui compromet le chiffrement.
 */
@Component
public class AesGcmEncryptor {

    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmEncryptor(@Value("${nenecare.encryption.key}") String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException(
                "[NeneCare] NENECARE_ENCRYPTION_KEY est requise et ne peut pas être vide. " +
                "Générer avec : openssl rand -base64 32"
            );
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                "[NeneCare] NENECARE_ENCRYPTION_KEY n'est pas un Base64 valide. " +
                "Générer avec : openssl rand -base64 32"
            );
        }
        if (keyBytes.length != 32) {
            throw new IllegalStateException(String.format(
                "[NeneCare] NENECARE_ENCRYPTION_KEY doit être de 32 octets après décodage " +
                "Base64 (AES-256). Taille actuelle : %d octet(s). " +
                "Générer avec : openssl rand -base64 32",
                keyBytes.length
            ));
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Concaténer IV + chiffré (le tag GCM est inclus dans ciphertext par le JCE)
            byte[] combined = new byte[IV_LENGTH_BYTES + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH_BYTES);
            System.arraycopy(ciphertext, 0, combined, IV_LENGTH_BYTES, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new EncryptionException("Erreur lors du chiffrement", e);
        }
    }

    public String decrypt(String base64Ciphertext) {
        try {
            byte[] combined = Base64.getDecoder().decode(base64Ciphertext);

            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(combined, IV_LENGTH_BYTES, combined.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Ne pas logguer le contenu — données médicales sensibles
            throw new EncryptionException("Erreur lors du déchiffrement (données corrompues ou mauvaise clé)", e);
        }
    }

    public static class EncryptionException extends RuntimeException {
        public EncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
