package sn.esp.nenecare.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Generation et validation des jetons JWT (US-02 : expiration 30 min).
 *
 * Proprietaire : Elimane (auth).
 *
 * Chaque jeton porte un identifiant unique (claim "jti") : c'est lui qui permet
 * la revocation a la deconnexion (US-03) sans invalider les jetons des autres
 * utilisateurs.
 */
@Service
public class JwtService {

    /** Taille minimale de la cle imposee par HMAC-SHA256. */
    private static final int TAILLE_MIN_SECRET = 32;

    private final SecretKey cle;
    private final long expirationMs;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration}") long expirationMs) {

        byte[] octets = secret.getBytes(StandardCharsets.UTF_8);
        if (octets.length < TAILLE_MIN_SECRET) {
            throw new IllegalStateException(
                "jwt.secret doit faire au moins " + TAILLE_MIN_SECRET
                + " caracteres (actuel : " + octets.length + "). "
                + "Definir la variable d'environnement JWT_SECRET.");
        }
        this.cle = Keys.hmacShaKeyFor(octets);
        this.expirationMs = expirationMs;
    }

    /** Genere un jeton signe contenant l'identifiant, le role et un jti unique. */
    public String genererToken(String username, String role) {
        Date maintenant = new Date();
        Date expiration = new Date(maintenant.getTime() + expirationMs);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
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

    /** Identifiant unique du jeton (claim "jti"), utilise pour la revocation (US-03). */
    public String extraireJti(String token) {
        return parser(token).getId();
    }

    public Date extraireExpiration(String token) {
        return parser(token).getExpiration();
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
