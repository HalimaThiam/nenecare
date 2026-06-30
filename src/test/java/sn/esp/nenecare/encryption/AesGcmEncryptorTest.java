package sn.esp.nenecare.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

class AesGcmEncryptorTest {

    // 32 octets de zéros encodés en Base64 — clé valide AES-256 pour les tests uniquement
    private static final String TEST_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    private AesGcmEncryptor encryptor;

    @BeforeEach
    void setUp() {
        encryptor = new AesGcmEncryptor(TEST_KEY);
    }

    // ── 1. Correction fonctionnelle (round-trip) ──────────────────────────────

    @Test
    void chiffrer_puis_dechiffrer_restitue_le_texte_original() {
        String texte = "Diagnostic : hypertension artérielle — stade II";

        String chiffre = encryptor.encrypt(texte);
        String resultat = encryptor.decrypt(chiffre);

        assertThat(resultat).isEqualTo(texte);
    }

    @ParameterizedTest(name = "[{index}] texte = \"{0}\"")
    @ValueSource(strings = {
        "Allergie à la pénicilline",
        "Traitement : paracétamol 1g × 3/j",
        "Notes : RAS",
        // Unicode médical, caractères multi-octets UTF-8
        "Antécédents : 糖尿病 (diabète) — niveau α",
        // Texte long (> 1 bloc AES)
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam."
    })
    void round_trip_sur_textes_varies(String texte) {
        assertThat(encryptor.decrypt(encryptor.encrypt(texte))).isEqualTo(texte);
    }

    // ── 2. IV aléatoire — même texte ≠ même chiffré ──────────────────────────

    @Test
    void deux_chiffrements_du_meme_texte_produisent_des_chiffres_differents() {
        String texte = "Groupe sanguin : A+";

        String chiffre1 = encryptor.encrypt(texte);
        String chiffre2 = encryptor.encrypt(texte);

        assertThat(chiffre1).isNotEqualTo(chiffre2);
    }

    @Test
    void les_iv_de_deux_chiffrements_sont_differents() {
        String texte = "Groupe sanguin : A+";

        byte[] combined1 = Base64.getDecoder().decode(encryptor.encrypt(texte));
        byte[] combined2 = Base64.getDecoder().decode(encryptor.encrypt(texte));

        // Les 12 premiers octets sont l'IV
        assertThat(combined1).doesNotStartWith(java.util.Arrays.copyOfRange(combined2, 0, 12));
    }

    // ── 3. Intégrité GCM — toute altération doit être détectée ───────────────

    @Test
    void dechiffrement_echoue_si_le_tag_gcm_est_altere() {
        byte[] combined = Base64.getDecoder().decode(encryptor.encrypt("données sensibles"));

        // Le tag GCM occupe les 16 derniers octets — en altérer un bit suffit
        combined[combined.length - 1] ^= 0xFF;
        String altere = Base64.getEncoder().encodeToString(combined);

        assertThatThrownBy(() -> encryptor.decrypt(altere))
            .isInstanceOf(AesGcmEncryptor.EncryptionException.class);
    }

    @Test
    void dechiffrement_echoue_si_le_corps_du_chiffre_est_altere() {
        byte[] combined = Base64.getDecoder().decode(encryptor.encrypt("données sensibles"));

        // Altérer un octet au milieu du chiffré (après IV[12], avant tag[16])
        int positionCorps = 12 + (combined.length - 12 - 16) / 2;
        combined[positionCorps] ^= 0x01;
        String altere = Base64.getEncoder().encodeToString(combined);

        assertThatThrownBy(() -> encryptor.decrypt(altere))
            .isInstanceOf(AesGcmEncryptor.EncryptionException.class);
    }

    @Test
    void dechiffrement_echoue_si_l_iv_est_altere() {
        byte[] combined = Base64.getDecoder().decode(encryptor.encrypt("données sensibles"));

        // Altérer le premier octet de l'IV modifie le compteur GCM → le tag ne correspond plus
        combined[0] ^= 0xFF;
        String altere = Base64.getEncoder().encodeToString(combined);

        assertThatThrownBy(() -> encryptor.decrypt(altere))
            .isInstanceOf(AesGcmEncryptor.EncryptionException.class);
    }

    // ── 4. Format du chiffré ──────────────────────────────────────────────────

    @Test
    void le_chiffre_respecte_le_format_iv_corps_tag() {
        // Format attendu : Base64( IV[12] || chiffré[N] || tag[16] )
        // Pour "test" (4 octets UTF-8) : 12 + 4 + 16 = 32 octets décodés
        String texte = "test";

        byte[] combined = Base64.getDecoder().decode(encryptor.encrypt(texte));
        int tailleAttendue = 12 + texte.getBytes(java.nio.charset.StandardCharsets.UTF_8).length + 16;

        assertThat(combined.length).isEqualTo(tailleAttendue);
    }

    // ── 5. Validation de la clé au démarrage ─────────────────────────────────

    @Test
    void cle_trop_courte_refuse_au_demarrage() {
        // 16 octets = AES-128, non accepté (on exige AES-256 = 32 octets)
        String cle128bits = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> new AesGcmEncryptor(cle128bits))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("32 octets");
    }

    @Test
    void cle_trop_longue_refuse_au_demarrage() {
        String cle384bits = Base64.getEncoder().encodeToString(new byte[48]);

        assertThatThrownBy(() -> new AesGcmEncryptor(cle384bits))
            .isInstanceOf(IllegalStateException.class);
    }
}
