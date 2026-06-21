package sn.esp.nenecare.patient.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dossier medical d'une patiente (mere).
 * STUB de base - a completer par Amadou.
 *
 * Regles de securite a respecter :
 *  - les champs medicaux sensibles sont stockes CHIFFRES (AES-256-GCM, OS-03)
 *    via AesGcmService avant persistance ;
 *  - une signature HMAC est calculee a la creation et verifiee a chaque lecture
 *    (OS-07) via HmacService.
 */
@Entity
@Table(name = "dossiers_medicaux")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DossierMedical {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String numeroDossier;

    @Column(nullable = false, length = 150)
    private String nomPatiente;

    /** Identifiant du gynecologue assigne (DAC : acces limite a ses patientes). */
    @Column(name = "gynecologue_assigne")
    private String gynecologueAssigne;

    /** Contenu medical sensible, stocke chiffre (Base64 du chiffre AES-GCM). */
    @Column(name = "contenu_chiffre", columnDefinition = "TEXT")
    private String contenuChiffre;

    /** Signature HMAC-SHA256 pour la verification d'integrite (OS-07). */
    @Column(name = "signature_hmac", length = 500)
    private String signatureHmac;

    @Column(name = "cree_le")
    private LocalDateTime creeLe;
}
