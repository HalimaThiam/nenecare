package sn.esp.nenecare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;
import sn.esp.nenecare.auth.jwt.JwtAuthFilter;
import sn.esp.nenecare.common.exception.RestAccessDeniedHandler;
import sn.esp.nenecare.common.exception.RestAuthenticationEntryPoint;

/**
 * Configuration de securite centrale (RBAC + sessions stateless JWT).
 *
 * Fichier PARTAGE : proprietaires Halima (architecte securite) + Elimane (auth).
 * @EnableMethodSecurity active @PreAuthorize sur les controleurs de chaque membre.
 *
 * Chaine de traitement d'une requete :
 *   JwtAuthFilter (lit le Bearer, remplit le SecurityContext)
 *     -> regles d'URL ci-dessous
 *     -> @PreAuthorize sur la methode du controleur
 *     -> 401 (RestAuthenticationEntryPoint) ou 403 (RestAccessDeniedHandler) en JSON
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    /** Encodeur bcrypt cout 12 (OS-12, exigence donnees medicales). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // API REST + JWT : CSRF non pertinent
            .csrf(csrf -> csrf.disable())
            // Aucune session serveur : tout repose sur le JWT
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Pages et ressources statiques (frontend Hadja).
                // Ce sont des coquilles vides : elles ne contiennent aucune
                // donnee, tout passe par des appels API authentifies. Les
                // proteger n'apporterait rien et empecherait d'afficher un
                // ecran de connexion propre.
                .requestMatchers("/", "/index.html", "/login.html", "/accueil.html",
                                 "/dossiers.html", "/audit.html",
                                 "/css/**", "/js/**",
                                 "/favicon.ico", "/favicon.svg").permitAll()
                // Console H2 : presente uniquement quand le profil dev l'active
                .requestMatchers("/h2-console/**").permitAll()
                // Connexion / deconnexion : necessairement ouvertes
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/logout").permitAll()
                // Journal d'audit : reserve a l'administrateur (US-16, US-17)
                .requestMatchers("/api/audit/**").hasRole("ADMIN")
                // Tout le reste exige une authentification
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            // La console H2 s'affiche dans une frame : autorisee sur la meme origine
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }
}
