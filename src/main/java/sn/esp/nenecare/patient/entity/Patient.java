package sn.esp.nenecare.patient.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import sn.esp.nenecare.encryption.EncryptedStringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "patients")
@Getter @Setter @NoArgsConstructor
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Non chiffré : identifiant opérationnel (recherche, affichage listes)
    @Column(name = "numero_dossier", unique = true, nullable = false, updatable = false)
    private String numeroDossier;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "nom", columnDefinition = "TEXT", nullable = false)
    private String nom;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "prenom", columnDefinition = "TEXT", nullable = false)
    private String prenom;

    // Non chiffré : nécessaire pour les calculs d'âge et alertes pédiatriques
    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    // Non chiffré : faible sensibilité, utile pour filtres statistiques
    @Column(name = "sexe", length = 10)
    private String sexe;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "telephone", columnDefinition = "TEXT")
    private String telephone;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "adresse", columnDefinition = "TEXT")
    private String adresse;

    @CreationTimestamp
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;
}
