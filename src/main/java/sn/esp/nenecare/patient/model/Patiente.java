package sn.esp.nenecare.patient.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Patiente (mere) suivie a la Clinique Marose - US-05, US-06.
 *
 * Proprietaire : Amadou (patient / crypto).
 *
 * Deux categories de champs, traitees differemment :
 *
 *  - IDENTITE ADMINISTRATIVE (nom, prenom, date de naissance, numero de
 *    dossier) : stockee en clair. La secretaire doit pouvoir enregistrer et
 *    rechercher une patiente sans acceder au moindre element medical.
 *
 *  - COORDONNEES ET DONNEES DE SANTE (telephone, adresse, groupe sanguin,
 *    antecedents) : stockees CHIFFREES en AES-256-GCM (OS-03). Une copie de
 *    la base derobee ne livre ni le carnet d'adresses des patientes, ni leur
 *    dossier de sante.
 *
 * Les champs suffixes "Chiffre" contiennent du Base64 (IV + texte chiffre) :
 * ils ne sont JAMAIS exposes tels quels par l'API. Le passage clair/chiffre
 * est fait par PatienteService via ProtectionDonneesService.
 */
@Entity
@Table(name = "patientes", indexes = {
        @Index(name = "idx_patiente_numero", columnList = "numero_patiente", unique = true),
        @Index(name = "idx_patiente_gyneco", columnList = "gynecologue_assigne")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patiente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifiant metier affiche a l'accueil (ex. PAT-2026-0001). */
    @Column(name = "numero_patiente", nullable = false, unique = true, length = 40)
    private String numeroPatiente;

    @Column(nullable = false, length = 80)
    private String nom;

    @Column(nullable = false, length = 80)
    private String prenom;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    // -------------------------------------------------------------------------
    // Champs chiffres (AES-256-GCM) - OS-03
    // -------------------------------------------------------------------------

    @Column(name = "telephone_chiffre", columnDefinition = "TEXT")
    private String telephoneChiffre;

    @Column(name = "adresse_chiffre", columnDefinition = "TEXT")
    private String adresseChiffre;

    @Column(name = "groupe_sanguin_chiffre", columnDefinition = "TEXT")
    private String groupeSanguinChiffre;

    @Column(name = "antecedents_chiffre", columnDefinition = "TEXT")
    private String antecedentsChiffre;

    // -------------------------------------------------------------------------
    // Controle d'acces discretionnaire (DAC)
    // -------------------------------------------------------------------------

    /**
     * Identifiant du gynecologue referent (US-07).
     * Un gynecologue ne consulte que les dossiers des patientes qui lui sont
     * assignees ; l'acces a une autre patiente passe par la procedure
     * d'urgence, tracee (US-14).
     */
    @Column(name = "gynecologue_assigne", length = 100)
    private String gynecologueAssigne;

    // -------------------------------------------------------------------------
    // Tracabilite
    // -------------------------------------------------------------------------

    @Column(name = "cree_par", length = 100)
    private String creePar;

    @Column(name = "cree_le")
    private LocalDateTime creeLe;

    @Column(name = "modifie_le")
    private LocalDateTime modifieLe;

    /** Nom affichable, utilise dans les libelles et les entrees d'audit. */
    public String nomComplet() {
        return prenom + " " + nom;
    }
}
