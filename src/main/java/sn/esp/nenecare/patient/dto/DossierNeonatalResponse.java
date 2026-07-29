package sn.esp.nenecare.patient.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

/** Vue d'un dossier neonatal renvoyee par l'API - contenu DECHIFFRE. */
@Data
@Builder
public class DossierNeonatalResponse {

    private Long id;
    private String numeroDossier;

    /** Liaison mere-enfant (OS-06) : toujours renseignee. */
    private Long mereId;
    private String mereNom;
    private String mereNumero;

    private String nomBebe;
    private String sexe;
    private LocalDate dateNaissance;

    private Integer poidsGrammes;
    private Integer tailleCm;
    private Integer scoreApgar;

    private String diagnostic;
    private String observations;

    private String statut;

    /** Resultat de la verification de signature a la lecture (OS-07). */
    private boolean integre;

    private String creePar;
    private LocalDateTime creeLe;
    private String modifiePar;
    private LocalDateTime modifieLe;
}
