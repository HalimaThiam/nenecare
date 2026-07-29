package sn.esp.nenecare.patient.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

/**
 * Vue d'un dossier medical renvoyee par l'API - contenu DECHIFFRE.
 *
 * Le champ {@code integre} porte le resultat de la verification HMAC faite a
 * la lecture (OS-07) : le soignant voit immediatement si le dossier qu'il
 * consulte a ete modifie hors de l'application.
 */
@Data
@Builder
public class DossierMedicalResponse {

    private Long id;
    private String numeroDossier;

    private Long patienteId;
    private String patienteNom;
    private String patienteNumero;

    private LocalDate dateConsultation;
    private String motif;

    private String diagnostic;
    private String traitement;
    private String observations;

    private String statut;

    /** Resultat de la verification de signature a la lecture (OS-07). */
    private boolean integre;

    private String creePar;
    private LocalDateTime creeLe;
    private String modifiePar;
    private LocalDateTime modifieLe;
}
