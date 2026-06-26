package sn.esp.nenecare.nenecare.crypto;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import sn.esp.nenecare.nenecare.model.Utilisateur;

@Service
public class JwtService {

    private static final String SECRET = "nenecare-jwt-secret-clinique-marose-ouakam-2026-secure-key";
    private static final long EXPIRATION = 1800000; // 30 minutes

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    // Génère un token JWT pour un utilisateur
    public String generateToken(Utilisateur utilisateur) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", utilisateur.getRole().name());
        claims.put("nom", utilisateur.getNom());
        claims.put("prenom", utilisateur.getPrenom());

        return Jwts.builder()
            .claims(claims)
            .subject(utilisateur.getIdentifiant())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
            .signWith(getSigningKey())
            .compact();
    }

    // Extrait l'identifiant depuis le token
    public String extractIdentifiant(String token) {
        return extractClaims(token).getSubject();
    }

    // Extrait le rôle depuis le token
    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    // Vérifie si le token est valide et non expiré
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}