package sn.esp.nenecare.auth.jwt;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Authentifie chaque requete a partir de l'en-tete "Authorization: Bearer <jeton>".
 *
 * Le filtre ne rejette rien lui-meme : si le jeton est absent ou invalide, il
 * laisse le SecurityContext vide et c'est la chaine Spring Security qui repond
 * 401 (RestAuthenticationEntryPoint) ou 403 selon la regle de l'URL demandee.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String EN_TETE = "Authorization";
    private static final String PREFIXE = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final JetonRevoqueService jetonRevoqueService;

    @Override
    protected void doFilterInternal(HttpServletRequest requete,
                                    HttpServletResponse reponse,
                                    FilterChain chaine) throws ServletException, IOException {

        String token = extraireToken(requete);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authentifier(token, requete);
        }

        chaine.doFilter(requete, reponse);
    }

    private void authentifier(String token, HttpServletRequest requete) {
        try {
            if (!jwtService.estValide(token)) {
                return;
            }

            // Jeton revoque par une deconnexion explicite (US-03).
            if (jetonRevoqueService.estRevoque(jwtService.extraireJti(token))) {
                return;
            }

            String username = jwtService.extraireUsername(token);
            UserDetails utilisateur = userDetailsService.loadUserByUsername(username);

            // Compte desactive entre-temps (US-19) : le jeton ne doit plus rien ouvrir.
            if (!utilisateur.isEnabled()) {
                return;
            }

            var authentification = new UsernamePasswordAuthenticationToken(
                    utilisateur, null, utilisateur.getAuthorities());
            authentification.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(requete));

            SecurityContextHolder.getContext().setAuthentication(authentification);

        } catch (Exception e) {
            // Jeton illisible, expire, ou utilisateur supprime : requete anonyme.
            SecurityContextHolder.clearContext();
        }
    }

    private String extraireToken(HttpServletRequest requete) {
        String enTete = requete.getHeader(EN_TETE);
        if (enTete != null && enTete.startsWith(PREFIXE)) {
            return enTete.substring(PREFIXE.length()).trim();
        }
        return null;
    }
}
