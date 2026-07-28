package sn.esp.nenecare.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;

import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.pqc.jcajce.spec.KyberParameterSpec;
import org.springframework.stereotype.Service;

/**
 * Echange de cles post-quantique Kyber-768 (bonus, au-dela des exigences de la charte).
 *
 * Proprietaire : Amadou (crypto).
 */
@Service
public class KyberService {

    static {
        if (Security.getProvider("BCPQC") == null) {
            Security.addProvider(new BouncyCastlePQCProvider());
        }
    }

    /** Genere une paire de cles Kyber-768. */
    public KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("Kyber", "BCPQC");
        keyPairGen.initialize(KyberParameterSpec.kyber768, new SecureRandom());
        return keyPairGen.generateKeyPair();
    }

    public String publicKeyToBase64(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public String privateKeyToBase64(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }
}
