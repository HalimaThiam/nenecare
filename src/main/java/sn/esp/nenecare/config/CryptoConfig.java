package sn.esp.nenecare.config;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Enregistre le fournisseur cryptographique Bouncy Castle au demarrage.
 * Centralise l'init du provider pour eviter de le repeter dans chaque service crypto.
 *
 * Proprietaire : Amadou (crypto / dossiers).
 */
@Configuration
public class CryptoConfig {

    @PostConstruct
    public void registerBouncyCastle() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
}
