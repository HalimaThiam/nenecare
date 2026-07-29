package sn.esp.nenecare.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Services cryptographiques : AES-256-GCM (OS-03) et HMAC-SHA256 (OS-07).
 *
 * Contexte Spring complet car ces services dependent du fournisseur Bouncy
 * Castle enregistre par CryptoConfig au demarrage.
 */
@SpringBootTest
class CryptoServicesTest {

    @Autowired private AesGcmService aesGcmService;
    @Autowired private HmacService hmacService;

    // =========================================================================
    // AES-256-GCM (OS-03)
    // =========================================================================

    @Test
    @DisplayName("OS-03 : un texte chiffre puis dechiffre revient identique")
    void chiffrementReversible() throws Exception {
        SecretKey cle = aesGcmService.generateKey();
        String enClair = "Groupe sanguin : O+, allergie penicilline, terme 38 SA";

        String chiffre = aesGcmService.encrypt(enClair, cle);

        assertThat(chiffre).isNotEqualTo(enClair);
        assertThat(aesGcmService.decrypt(chiffre, cle)).isEqualTo(enClair);
    }

    @Test
    @DisplayName("Le chiffre ne laisse pas fuir le texte en clair")
    void chiffreNeContientPasLeClair() throws Exception {
        SecretKey cle = aesGcmService.generateKey();

        String chiffre = aesGcmService.encrypt("Fatou NDIAYE", cle);

        assertThat(chiffre).doesNotContain("Fatou").doesNotContain("NDIAYE");
    }

    @Test
    @DisplayName("Deux chiffrements du meme texte different (IV aleatoire)")
    void ivAleatoireParMessage() throws Exception {
        SecretKey cle = aesGcmService.generateKey();

        // Sans IV aleatoire, deux dossiers au contenu identique produiraient
        // le meme chiffre : on saurait qu'ils sont identiques sans les lire.
        assertThat(aesGcmService.encrypt("meme contenu", cle))
                .isNotEqualTo(aesGcmService.encrypt("meme contenu", cle));
    }

    @Test
    @DisplayName("Une mauvaise cle ne permet pas de dechiffrer")
    void mauvaiseCleRejetee() throws Exception {
        String chiffre = aesGcmService.encrypt("donnee medicale", aesGcmService.generateKey());
        SecretKey autreCle = aesGcmService.generateKey();

        assertThatThrownBy(() -> aesGcmService.decrypt(chiffre, autreCle))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("GCM detecte l'alteration du chiffre")
    void alterationDuChiffreDetectee() throws Exception {
        SecretKey cle = aesGcmService.generateKey();
        String chiffre = aesGcmService.encrypt("tension 12/8, poids 3200g", cle);

        // On modifie un caractere du Base64 : le tag d'authentification GCM
        // doit faire echouer le dechiffrement plutot que rendre des octets faux.
        char premier = chiffre.charAt(10);
        String altere = chiffre.substring(0, 10)
                      + (premier == 'A' ? 'B' : 'A')
                      + chiffre.substring(11);

        assertThatThrownBy(() -> aesGcmService.decrypt(altere, cle))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Une cle exportee en Base64 se recharge a l'identique")
    void cleSerialisable() throws Exception {
        SecretKey cle = aesGcmService.generateKey();
        String chiffre = aesGcmService.encrypt("contenu", cle);

        SecretKey rechargee = aesGcmService.keyFromBase64(aesGcmService.keyToBase64(cle));

        assertThat(aesGcmService.decrypt(chiffre, rechargee)).isEqualTo("contenu");
    }

    // =========================================================================
    // HMAC-SHA256 (OS-07)
    // =========================================================================

    @Test
    @DisplayName("OS-07 : une signature valide est reconnue, une donnee modifiee ne l'est pas")
    void signatureEtVerification() throws Exception {
        String cle = hmacService.generateKey();
        String donnee = "dossier#42|gynecologue|2026-07-28";

        String signature = hmacService.sign(donnee, cle);

        assertThat(hmacService.verify(donnee, cle, signature)).isTrue();
        assertThat(hmacService.verify(donnee + " ", cle, signature)).isFalse();
        assertThat(hmacService.verify(donnee, cle, null)).isFalse();
    }

    @Test
    @DisplayName("Une signature produite avec une autre cle est rejetee")
    void signatureAvecAutreCleRejetee() throws Exception {
        String donnee = "dossier#42";
        String signature = hmacService.sign(donnee, hmacService.generateKey());

        assertThat(hmacService.verify(donnee, hmacService.generateKey(), signature)).isFalse();
    }

    @Test
    @DisplayName("La signature est reproductible pour une meme cle et une meme donnee")
    void signatureDeterministe() throws Exception {
        String cle = hmacService.generateKey();

        assertThat(hmacService.sign("donnee", cle))
                .isEqualTo(hmacService.sign("donnee", cle));
    }

    @Test
    @DisplayName("Les caracteres accentues sont signes de facon stable (UTF-8)")
    void signatureStableSurCaracteresAccentues() throws Exception {
        String cle = hmacService.generateKey();
        String donnee = "Néné · dossier néonatal · suivi prénatal";

        assertThat(hmacService.verify(donnee, cle, hmacService.sign(donnee, cle))).isTrue();
    }

    @Test
    @DisplayName("Une cle HMAC generee fait bien 256 bits")
    void cleHmacDe256Bits() {
        assertThat(java.util.Base64.getDecoder().decode(hmacService.generateKey()))
                .hasSize(32);
    }
}
