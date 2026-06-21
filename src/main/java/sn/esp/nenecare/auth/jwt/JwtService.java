package sn.esp.nenecare.auth.jwt;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Generation et validation des jetons JWT (US-02 : expiration 30 min).
 *
 * Proprietaire : Elimane (auth). Squelette fonctionnel a completer.
 */
@Service
public class JwtService {

    private final SecretKey cle;
    private final long expirationMs;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration}") long expirationMs) {
        // La cle doit faire >= 256 bits pour HMAC-SHA256
        this.cle = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    /** Genere un jeton signe contenant l'identifiant et le role. */
    public String genererToken(String username, String role) {
        Date maintenant = new Date();
        Date expiration = new Date(maintenant.getTime() + expirationMs);
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(maintenant)
                .expiration(expiration)
                .signWith(cle)
                .compact();
    }

    /** Extrait l'identifiant (subject) ; leve une exception si le jeton est invalide/expire. */
    public String extraireUsername(String token) {
        return parser(token).getSubject();
    }

    public String extraireRole(String token) {
        return parser(token).get("role", String.class);
    }

    public boolean estValide(String token) {
        try {
            parser(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parser(String token) {
        return Jwts.parser()
                .verifyWith(cle)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
