package sn.esp.nenecare.user.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Compte d'un professionnel de sante de la Clinique Marose.
 * Le mot de passe est stocke hache (bcrypt) - jamais en clair (OS-12).
 *
 * Entite partagee : proprietaires Elimane (auth) + Halima (architecture).
 * Toute modification de structure doit etre coordonnee avec l'equipe.
 */
@Entity
@Table(name = "utilisateurs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    /** Mot de passe hache bcrypt (cout >= 12). */
    @Column(name = "mot_de_passe", nullable = false, length = 100)
    private String motDePasse;

    @Column(nullable = false, length = 100)
    private String nomComplet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    /** Compte actif : passe a false lors d'une revocation (US-19). */
    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;

    @Column(name = "cree_le")
    private LocalDateTime creeLe;
}
