package sn.esp.nenecare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration de securite centrale (RBAC + sessions stateless JWT).
 *
 * Fichier PARTAGE : proprietaires Halima (architecte securite) + Elimane (auth).
 * @EnableMethodSecurity active @PreAuthorize sur les controleurs de chaque membre.
 *
 * NOTE : tant que le JwtAuthFilter (Elimane) n'est pas branche, seuls /api/auth,
 * les ressources statiques et /api/audit (demo) sont ouverts. A durcir au Sprint Beta.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

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
                // Pages et ressources statiques (frontend Hadja)
                .requestMatchers("/", "/index.html", "/login.html",
                                 "/css/**", "/js/**", "/favicon.ico").permitAll()
                // Authentification ouverte (login / logout)
                .requestMatchers("/api/auth/**").permitAll()
                // Audit : ouvert temporairement pour la demo Sprint Alpha
                .requestMatchers("/api/audit/**").permitAll()
                // Tout le reste exige une authentification
                .anyRequest().authenticated());

        // TODO Elimane : http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
