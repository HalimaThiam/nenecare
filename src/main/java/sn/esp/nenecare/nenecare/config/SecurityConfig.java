package sn.esp.nenecare.nenecare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Encoder bcrypt avec coût 12 (recommandé pour données médicales)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Désactive CSRF pour API REST (JWT gère la sécurité)
            .csrf(csrf -> csrf.disable())

            // Stateless : pas de session serveur, JWT uniquement
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Règles d'accès
            .authorizeHttpRequests(auth -> auth
                // Page de login accessible à tous
                .requestMatchers("/api/auth/**").permitAll()
                // Tout le reste nécessite une authentification
                .anyRequest().authenticated()
            );

        return http.build();
    }
}