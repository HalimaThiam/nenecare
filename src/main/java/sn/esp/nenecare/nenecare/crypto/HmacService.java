package sn.esp.nenecare.nenecare.crypto;

import java.security.Security;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

@Service
public class HmacService {

    private static final String ALGORITHM = "HmacSHA256";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    // Génère une signature HMAC d'un dossier médical
    public String sign(String data, String secretKey) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(
            Base64.getDecoder().decode(secretKey), ALGORITHM
        );
        Mac mac = Mac.getInstance(ALGORITHM, "BC");
        mac.init(keySpec);
        byte[] signature = mac.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(signature);
    }

    // Vérifie l'intégrité d'un dossier à chaque lecture
    public boolean verify(String data, String secretKey, String expectedSignature) 
            throws Exception {
        String actualSignature = sign(data, secretKey);
        return actualSignature.equals(expectedSignature);
    }

    // Génère une clé HMAC aléatoire
    public String generateKey() {
        byte[] key = new byte[32];
        new java.security.SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}