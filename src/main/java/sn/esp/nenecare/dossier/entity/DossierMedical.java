package sn.esp.nenecare.dossier.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import sn.esp.nenecare.audit.DossierAuditListener;
import sn.esp.nenecare.encryption.EncryptedStringConverter;
import sn.esp.nenecare.patient.entity.Patient;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dossiers_medicaux")
@EntityListeners(DossierAuditListener.class)
public class DossierMedical {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false, updatable = false)
    private Patient patient;

    // ── Champs chiffrés (AES-256-GCM via JPA converter) ──────────────────────

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "antecedents_medicaux", columnDefinition = "TEXT")
    private String antecedentsMedicaux;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "diagnostics", columnDefinition = "TEXT")
    private String diagnostics;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "traitements", columnDefinition = "TEXT")
    private String traitements;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "allergies", columnDefinition = "TEXT")
    private String allergies;

    // Accès restreint : MEDECIN et ADMIN uniquement (contrôle dans le service)
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "notes_confidentielles", columnDefinition = "TEXT")
    private String notesConfidentielles;

    // ── Champ non chiffré ─────────────────────────────────────────────────────

    // Lisible en clair pour les urgences (transfusion, choc anaphylactique)
    @Column(name = "groupe_sanguin", length = 5)
    private String groupeSanguin;

    // ── Méta-données ──────────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "date_creation", updatable = false, nullable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_mise_a_jour")
    private LocalDateTime dateMiseAJour;

    @Column(name = "cree_par", updatable = false, nullable = false, length = 100)
    private String creePar;

    @Column(name = "modifie_par", length = 100)
    private String modifiePar;

    // Verrou optimiste — prévient les écrasements concurrents de dossiers
    @Version
    private Long version;

    public DossierMedical() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public String getAntecedentsMedicaux() { return antecedentsMedicaux; }
    public void setAntecedentsMedicaux(String v) { this.antecedentsMedicaux = v; }

    public String getDiagnostics() { return diagnostics; }
    public void setDiagnostics(String v) { this.diagnostics = v; }

    public String getTraitements() { return traitements; }
    public void setTraitements(String v) { this.traitements = v; }

    public String getAllergies() { return allergies; }
    public void setAllergies(String v) { this.allergies = v; }

    public String getNotesConfidentielles() { return notesConfidentielles; }
    public void setNotesConfidentielles(String v) { this.notesConfidentielles = v; }

    public String getGroupeSanguin() { return groupeSanguin; }
    public void setGroupeSanguin(String v) { this.groupeSanguin = v; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public LocalDateTime getDateMiseAJour() { return dateMiseAJour; }

    public String getCreePar() { return creePar; }
    public void setCreePar(String v) { this.creePar = v; }

    public String getModifiePar() { return modifiePar; }
    public void setModifiePar(String v) { this.modifiePar = v; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
