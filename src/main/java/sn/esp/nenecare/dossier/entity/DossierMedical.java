package sn.esp.nenecare.dossier.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
@Getter @Setter @NoArgsConstructor
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
}
