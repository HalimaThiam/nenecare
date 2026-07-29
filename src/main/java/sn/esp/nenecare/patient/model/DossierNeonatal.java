package sn.esp.nenecare.patient.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dossier du nouveau-ne - US-10 a US-13.
 *
 * Proprietaire : Amadou (patient / crypto).
 *
 * LIAISON MERE-ENFANT (OS-06) : chaque dossier neonatal pointe vers la
 * patiente mere via une association @ManyToOne obligatoire. Une mere peut
 * avoir plusieurs enfants (grossesses successives ou multiples), un dossier
 * neonatal a toujours exactement une mere. La contrainte de cle etrangere
 * garantit qu'aucun dossier d'enfant ne peut exister sans mere rattachee -
 * c'est la regle qui rend le suivi mere-enfant fiable.
 *
 * Le pediatre accede au dossier de l'enfant ; le lien lui donne le contexte
 * obstetrical sans dupliquer les donnees de la mere.
 */
@Entity
@Table(name = "dossiers_neonatals", indexes = {
        @Index(name = "idx_neonat_numero", columnList = "numero_dossier", unique = true),
        @Index(name = "idx_neonat_mere",   columnList = "mere_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DossierNeonatal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifiant metier du dossier (ex. DN-2026-0001). */
    @Column(name = "numero_dossier", nullable = false, unique = true, length = 40)
    private String numeroDossier;

    /** La mere. optional = false : pas de dossier neonatal orphelin (OS-06). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mere_id", nullable = false)
    private Patiente mere;

    @Column(name = "nom_bebe", nullable = false, length = 120)
    private String nomBebe;

    /** M ou F. */
    @Column(name = "sexe", length = 1)
    private String sexe;

    @Column(name = "date_naissance", nullable = false)
    private LocalDate dateNaissance;

    // -------------------------------------------------------------------------
    // Mesures a la naissance
    // -------------------------------------------------------------------------
    // Conservees en clair : ce sont les seules valeurs sur lesquelles la
    // clinique produit des statistiques agregees (poids moyen, prematurite).
    // Isolees, elles n'identifient personne ; c'est leur rattachement au nom
    // qui serait sensible, et ce lien passe par la table chiffree de la mere.

    @Column(name = "poids_grammes")
    private Integer poidsGrammes;

    @Column(name = "taille_cm")
    private Integer tailleCm;

    /** Score d'Apgar a 5 minutes (0 a 10). */
    @Column(name = "score_apgar")
    private Integer scoreApgar;

    // -------------------------------------------------------------------------
    // Contenu medical chiffre (AES-256-GCM) - OS-03
    // -------------------------------------------------------------------------

    @Column(name = "diagnostic_chiffre", columnDefinition = "TEXT")
    private String diagnosticChiffre;

    @Column(name = "observations_chiffre", columnDefinition = "TEXT")
    private String observationsChiffre;

    // -------------------------------------------------------------------------
    // Integrite (OS-07)
    // -------------------------------------------------------------------------

    @Column(name = "signature_hmac", length = 200)
    private String signatureHmac;

    // -------------------------------------------------------------------------
    // Cycle de vie et tracabilite
    // -------------------------------------------------------------------------

    @Column(name = "statut", nullable = false, length = 20)
    @Builder.Default
    private String statut = StatutDossier.ACTIF;

    @Column(name = "cree_par", length = 100)
    private String creePar;

    @Column(name = "cree_le")
    private LocalDateTime creeLe;

    @Column(name = "modifie_par", length = 100)
    private String modifiePar;

    @Column(name = "modifie_le")
    private LocalDateTime modifieLe;
}
