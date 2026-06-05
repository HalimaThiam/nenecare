package sn.esp.nenecare.nenecare.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.pqc.jcajce.spec.KyberParameterSpec;
import org.springframework.stereotype.Service;

@Service
public class KyberService {

    static {
        Security.addProvider(new BouncyCastleProvider());
        Security.addProvider(new BouncyCastlePQCProvider());
    }

    // Génère une paire de clés Kyber-768
    public KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(
            "Kyber", "BCPQC"
        );
        keyPairGen.initialize(KyberParameterSpec.kyber768, new SecureRandom());
        return keyPairGen.generateKeyPair();
    }

    // Encode la clé publique en Base64 pour échange
    public String publicKeyToBase64(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    // Encode la clé privée en Base64 pour stockage sécurisé
    public String privateKeyToBase64(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }
}