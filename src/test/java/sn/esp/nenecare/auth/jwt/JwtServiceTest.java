package sn.esp.nenecare.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Generation et validation des jetons JWT (US-02).
 *
 * Teste sans contexte Spring : le service est instancie directement, ce qui
 * permet de faire varier le secret et la duree d'expiration.
 */
class JwtServiceTest {

    private static final String SECRET       = "secret-de-test-de-32-caracteres-minimum-pour-hmac";
    private static final String AUTRE_SECRET = "un-tout-autre-secret-de-32-caracteres-minimum-ok";
    private static final long   TRENTE_MIN   = Duration.ofMinutes(30).toMillis();

    @Test
    @DisplayName("Le jeton porte l'identifiant et le role")
    void genereUnJetonExploitable() {
        JwtService service = new JwtService(SECRET, TRENTE_MIN);

        String jeton = service.genererToken("gyneco.test", "GYNECOLOGUE");

        assertThat(service.estValide(jeton)).isTrue();
        assertThat(service.extraireUsername(jeton)).isEqualTo("gyneco.test");
        assertThat(service.extraireRole(jeton)).isEqualTo("GYNECOLOGUE");
    }

    @Test
    @DisplayName("US-02 : l'expiration est fixee a 30 minutes")
    void expirationTrenteMinutes() {
        JwtService service = new JwtService(SECRET, TRENTE_MIN);

        Date expiration = service.extraireExpiration(
                service.genererToken("gyneco.test", "GYNECOLOGUE"));

        long minutes = Duration.ofMillis(expiration.getTime() - System.currentTimeMillis())
                               .toMinutes();
        assertThat(minutes).isBetween(29L, 30L);
    }

    @Test
    @DisplayName("Un jeton expire est refuse")
    void jetonExpireRefuse() throws Exception {
        // Expiration deja passee : le jeton nait perime.
        JwtService service = new JwtService(SECRET, -1000L);

        String jeton = service.genererToken("gyneco.test", "GYNECOLOGUE");

        assertThat(service.estValide(jeton)).isFalse();
    }

    @Test
    @DisplayName("Un jeton signe avec un autre secret est refuse")
    void signatureEtrangereRefusee() {
        String jeton = new JwtService(AUTRE_SECRET, TRENTE_MIN)
                .genererToken("attaquant", "ADMIN");

        assertThat(new JwtService(SECRET, TRENTE_MIN).estValide(jeton)).isFalse();
    }

    @Test
    @DisplayName("Un jeton bricole a la main est refuse")
    void jetonAlteRefuse() {
        JwtService service = new JwtService(SECRET, TRENTE_MIN);
        String jeton = service.genererToken("infirmier", "INFIRMIER");

        // On modifie un caractere du corps : la signature ne colle plus.
        String[] parties = jeton.split("\\.");
        String altere = parties[0] + "." + parties[1] + "x." + parties[2];

        assertThat(service.estValide(altere)).isFalse();
        assertThat(service.estValide("pas.un.jwt")).isFalse();
        assertThat(service.estValide("")).isFalse();
    }

    @Test
    @DisplayName("Chaque jeton porte un identifiant unique (jti) pour la revocation")
    void jtiUniqueParJeton() {
        JwtService service = new JwtService(SECRET, TRENTE_MIN);

        String premier = service.extraireJti(service.genererToken("a", "ADMIN"));
        String second  = service.extraireJti(service.genererToken("a", "ADMIN"));

        assertThat(premier).isNotBlank();
        assertThat(premier).isNotEqualTo(second);
    }

    @Test
    @DisplayName("Un secret trop court fait echouer le demarrage, pas une requete")
    void secretTropCourtRefuseAuDemarrage() {
        assertThatThrownBy(() -> new JwtService("trop-court", TRENTE_MIN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.secret");
    }
}
