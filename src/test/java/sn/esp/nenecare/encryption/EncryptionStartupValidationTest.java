package sn.esp.nenecare.encryption;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie le comportement fail-fast de AesGcmEncryptor dans le contexte Spring.
 *
 * Chaque test utilise ApplicationContextRunner pour simuler le démarrage
 * de l'application avec différentes valeurs de NENECARE_ENCRYPTION_KEY,
 * sans démarrer un serveur HTTP ni une base de données.
 */
class EncryptionStartupValidationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(AesGcmEncryptor.class);

    // ── Cas d'échec — le contexte doit refuser de démarrer ───────────────────

    @Test
    void demarrage_echoue_si_propriete_absente() {
        // @Value("${nenecare.encryption.key}") sans default → Spring lève
        // IllegalArgumentException ("Could not resolve placeholder") avant même
        // d'appeler le constructeur.
        runner.run(context ->
            assertThat(context).hasFailed()
        );
    }

    @Test
    void demarrage_echoue_si_valeur_vide() {
        runner
            .withPropertyValues("nenecare.encryption.key=")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("NENECARE_ENCRYPTION_KEY");
            });
    }

    @Test
    void demarrage_echoue_si_base64_invalide() {
        // Contient des caractères hors alphabet Base64 standard (+, /, A-Z, a-z, 0-9)
        runner
            .withPropertyValues("nenecare.encryption.key=ceci_nest_pas_du_base64!!!")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Base64");
            });
    }

    @Test
    void demarrage_echoue_si_cle_trop_courte_aes128() {
        String cle16Octets = Base64.getEncoder().encodeToString(new byte[16]);
        runner
            .withPropertyValues("nenecare.encryption.key=" + cle16Octets)
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("32 octet");
            });
    }

    @Test
    void demarrage_echoue_si_cle_trop_longue() {
        String cle48Octets = Base64.getEncoder().encodeToString(new byte[48]);
        runner
            .withPropertyValues("nenecare.encryption.key=" + cle48Octets)
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("32 octet");
            });
    }

    // ── Cas nominal — le contexte doit démarrer ───────────────────────────────

    @Test
    void demarrage_reussit_avec_cle_aes256_valide() {
        String cleValide = Base64.getEncoder().encodeToString(new byte[32]);
        runner
            .withPropertyValues("nenecare.encryption.key=" + cleValide)
            .run(context ->
                assertThat(context).hasNotFailed()
            );
    }

    @Test
    void demarrage_reussit_et_le_bean_est_disponible_avec_cle_valide() {
        String cleValide = Base64.getEncoder().encodeToString(new byte[32]);
        runner
            .withPropertyValues("nenecare.encryption.key=" + cleValide)
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(AesGcmEncryptor.class);
            });
    }
}
