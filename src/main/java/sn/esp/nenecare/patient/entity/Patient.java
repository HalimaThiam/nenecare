package sn.esp.nenecare.patient.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import sn.esp.nenecare.encryption.EncryptedStringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "patients")
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

    public Patient() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNumeroDossier() { return numeroDossier; }
    public void setNumeroDossier(String v) { this.numeroDossier = v; }

    public String getNom() { return nom; }
    public void setNom(String v) { this.nom = v; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String v) { this.prenom = v; }

    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate v) { this.dateNaissance = v; }

    public String getSexe() { return sexe; }
    public void setSexe(String v) { this.sexe = v; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String v) { this.telephone = v; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String v) { this.adresse = v; }

    public LocalDateTime getDateCreation() { return dateCreation; }
}
